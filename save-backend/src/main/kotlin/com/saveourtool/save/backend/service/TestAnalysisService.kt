@file:Suppress("FILE_UNORDERED_IMPORTS")

package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.utils.asTestRun
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.Project
import com.saveourtool.save.entities.TestExecution
import com.saveourtool.save.test.analysis.api.TestId
import com.saveourtool.save.test.analysis.api.TestIdGenerator
import com.saveourtool.save.test.analysis.api.TestStatisticsStorage
import com.saveourtool.save.test.analysis.api.testId
import com.saveourtool.save.test.analysis.entities.metadata
import com.saveourtool.save.test.analysis.internal.MutableTestStatisticsStorage
import com.saveourtool.save.test.analysis.metrics.TestMetrics
import com.saveourtool.save.test.analysis.results.AnalysisResult
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.info
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Spliterator.ORDERED
import java.util.Spliterator.SIZED
import java.util.Spliterators.AbstractSpliterator
import java.util.Spliterators.iterator
import java.util.function.Consumer
import java.util.stream.Stream
import java.util.stream.StreamSupport.stream
import kotlin.math.min
import kotlin.system.measureNanoTime
import com.saveourtool.save.test.analysis.api.TestAnalysisService as LowLevelAnalysisService

/**
 * The high-level test analysis service.
 *
 * @see LowLevelAnalysisService
 * @see TestStatisticsStorage
 */
@Service
class TestAnalysisService(
    private val statisticsStorage: MutableTestStatisticsStorage,
    private val testIdGenerator: TestIdGenerator,
    private val testExecutionService: TestExecutionService,
    private val executionService: ExecutionService,
    private val organizationService: OrganizationService,
    private val projectService: ProjectService,
) {
    @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
    private val logger = getLogger<TestAnalysisService>()

    private val lowLevelAnalysisService = LowLevelAnalysisService(statisticsStorage)

    @EventListener(ApplicationReadyEvent::class)
    fun init() {
        val testRunCount: Long

        @Suppress("Destructure")
        val nanos = measureNanoTime {
            testRunCount = organizationService.findAll()
                .parallelStream()
                .flatMap { organization ->
                    projectService.getAllByOrganizationName(organization.name)
                        .parallelStream()
                }
                .mapToLong(this::updateStatistics)
                .sum()
        }

        @Suppress("MagicNumber", "FLOAT_IN_ACCURATE_CALCULATIONS")
        logger.info {
            "Started ${javaClass.simpleName} ($testRunCount test run(s)) in ${nanos / 1000L / 1e3} ms."
        }
    }

    /**
     * Returns the aggregate statistics for the given [testId].
     *
     * @param testId the unique test id.
     * @return the scalar test metrics for the given [testId].
     * @see TestStatisticsStorage.getTestMetrics
     */
    @Suppress("GrazieInspection")
    fun getTestMetrics(testId: TestId): Mono<TestMetrics> =
            Mono.fromCallable {
                statisticsStorage.getTestMetrics(testId)
            }

    /**
     * Analyzes test runs for the given test [testId] and returns the [Flux] of
     * results.
     *
     * @param testId the unique test id.
     * @return the [Flux] of analysis results for the given test [testId].
     * @see LowLevelAnalysisService.analyze
     */
    fun analyze(testId: TestId): Flux<AnalysisResult> =
            Flux.fromStream {
                lowLevelAnalysisService.analyze(testId).stream()
            }

    /**
     * @return the number of test executions for this project.
     */
    private fun updateStatistics(project: Project): Long {
        /*
         * Process executions sequentially.
         */
        return executionService.getExecutionByNameAndOrganization(project.name, project.organization)
            .stream()
            .sequential()
            .sorted(Comparator.comparingLong(Execution::requiredId))
            .mapToLong { execution ->
                /*
                 * Process test executions in parallel (slightly faster).
                 */
                val testExecutions = testExecutionService.getTestExecutions(execution.requiredId())
                    .parallelStream()

                val metadataStream = execution.metadata().let { metadata ->
                    Stream.generate { metadata }
                }

                (testExecutions zip metadataStream)
                    .mapRight { testExecution, metadata ->
                        metadata.extendWith(testExecution)
                    }
                    .mapRight(testIdGenerator::testId)
                    .mapLeft(TestExecution::asTestRun)
                    .map { (testRun, testId) ->
                        statisticsStorage.updateExecutionStatistics(testId, testRun)
                    }
                    .count()
            }
            .sum()
    }

    private infix fun <T, R> Stream<T>.zip(other: Stream<R>): Stream<Pair<T, R>> {
        val isParallel = isParallel || other.isParallel
        val splitrLeft = spliterator()
        val splitrRight = other.spliterator()
        val characteristics = splitrLeft.characteristics() and
                splitrRight.characteristics() and
                (SIZED or ORDERED)
        val itrLeft = iterator(splitrLeft)
        val itrRight = iterator(splitrRight)

        return stream(
            object : AbstractSpliterator<Pair<T, R>>(
                min(splitrLeft.estimateSize(), splitrRight.estimateSize()),
                characteristics
            ) {
                override fun tryAdvance(action: Consumer<in Pair<T, R>>): Boolean =
                        when {
                            itrLeft.hasNext() && itrRight.hasNext() -> {
                                action.accept(itrLeft.next() to itrRight.next())
                                true
                            }

                            else -> false
                        }
            },
            isParallel,
        )
            .onClose { close() }
            .onClose { other.close() }
    }

    private fun <A : Any, B : Any, R : Any> Stream<Pair<A, B>>.mapLeft(transformLeft: (A, B) -> R): Stream<Pair<R, B>> =
            map { (left, right) ->
                transformLeft(left, right) to right
            }

    private fun <A : Any, B : Any, R : Any> Stream<Pair<A, B>>.mapLeft(transformLeft: (A) -> R): Stream<Pair<R, B>> =
            mapLeft { left, _ ->
                transformLeft(left)
            }

    private fun <A : Any, B : Any, R : Any> Stream<Pair<A, B>>.mapRight(transformRight: (A, B) -> R): Stream<Pair<A, R>> =
            map { (first, second) ->
                first to transformRight(first, second)
            }

    private fun <A : Any, B : Any, R : Any> Stream<Pair<A, B>>.mapRight(transformRight: (B) -> R): Stream<Pair<A, R>> =
            mapRight { _, right ->
                transformRight(right)
            }
}
