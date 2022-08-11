package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.TestExecutionRepository
import com.saveourtool.save.backend.repository.TestRepository
import com.saveourtool.save.backend.repository.TestSuiteRepository
import com.saveourtool.save.backend.storage.TestSuitesSourceSnapshotStorage
import com.saveourtool.save.backend.utils.blockingToFlux
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.testsuite.TestSuiteFilters
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.orNotFound
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.extra.math.max
import java.time.LocalDateTime

typealias TestSuiteDtoList = List<TestSuiteDto>

/**
 * Service for test suites
 */
@Service
class TestSuitesService(
    private val testSuiteRepository: TestSuiteRepository,
    private val testRepository: TestRepository,
    private val testExecutionRepository: TestExecutionRepository,
    private val testSuitesSourceService: TestSuitesSourceService,
    private val testSuitesSourceSnapshotStorage: TestSuitesSourceSnapshotStorage,
) {
    /**
     * Save new test suites to DB
     *
     * @param testSuitesDto test suites, that should be checked and possibly saved
     * @return list of *all* TestSuites
     */
    @Suppress("TOO_MANY_LINES_IN_LAMBDA", "UnsafeCallOnNullableType")
    fun saveTestSuite(testSuitesDto: List<TestSuiteDto>): List<TestSuite> {
        // FIXME: need to check logic about [dateAdded]
        // It's kind of upsert (insert or update) with key of all fields excluding [dateAdded]
        // This logic will be removed after https://github.com/saveourtool/save-cli/issues/429
        val testSuites = testSuitesDto
            .distinctBy {
                // Same suites may be declared in different directories, we unify them here.
                // We allow description of existing test suites to be changed.
                it.copy(description = null)
            }
            .map {
                TestSuite(
                    name = it.name,
                    description = it.description,
                    source = testSuitesSourceService.getByName(it.source.organizationName, it.source.name),
                    version = it.version,
                    dateAdded = null,
                    language = it.language,
                    tags = it.tags?.let(TestSuite::tagsFromList),
                )
            }
            .map { testSuite ->
                // try to find TestSuite in the DB based on all non-null properties of `testSuite`
                // NB: that's why `dateAdded` is null in the mapping above
                val description = testSuite.description
                testSuiteRepository
                    .findOne(
                        Example.of(testSuite.apply { this.description = null })
                    )
                    .orElseGet {
                        // if testSuite is not present in the DB, we will save it with current timestamp
                        testSuite.apply {
                            dateAdded = LocalDateTime.now()
                            this.description = description
                        }
                    }
            }
        testSuiteRepository.saveAll(testSuites)
        return testSuites.toList()
    }

    /**
     * @return all standard test suites
     */
    fun getStandardTestSuites(): Mono<TestSuiteDtoList> = blockingToFlux { testSuitesSourceService.getStandardTestSuitesSources() }
        .flatMap { testSuitesSource ->
            testSuitesSourceSnapshotStorage.list(testSuitesSource.organization.name, testSuitesSource.name)
                .max { max, next -> max.creationTimeInMills.compareTo(next.creationTimeInMills) }
                .map { testSuitesSource to it.version }
        }
        .flatMap { (testSuitesSource, version) ->
            blockingToFlux { testSuiteRepository.findAllBySourceAndVersion(testSuitesSource, version) }
        }
        .map { it.toDto() }
        .collectList()

    /**
     * @param id
     * @return test suite with [id]
     */
    fun findTestSuiteById(id: Long): TestSuite? = testSuiteRepository.findByIdOrNull(id)

    /**
     * @param ids
     * @return List of [TestSuite] by [ids]
     */
    fun findTestSuitesByIds(ids: List<Long>): Flux<TestSuite> = blockingToFlux {
        ids.mapNotNull { id ->
            testSuiteRepository.findByIdOrNull(id)
        }
    }

    /**
     * @param filters
     * @return [Flux] of [TestSuite] that match [filters]
     */
    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    fun findTestSuitesMatchingFilters(filters: TestSuiteFilters): Flux<TestSuite> =
            ExampleMatcher.matchingAll()
                .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase())
                // .withMatcher("language", ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase())
                .withMatcher("tags", ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase())
                .withIgnorePaths("description", "source", "version", "dateAdded", "language")
                .let {
                    Example.of(
                        TestSuite(
                            filters.name,
                            "",
                            TestSuitesSource.empty,
                            "",
                            null,
                            filters.language,
                            filters.tags
                        ),
                        it
                    )
                }
                .let { testSuiteRepository.findAll(it) }
                .toFlux()

    /**
     * @param id
     * @return test suite with [id]
     * @throws ResponseStatusException if [TestSuite] is not found by [id]
     */
    fun getById(id: Long) = testSuiteRepository.findByIdOrNull(id)
        .orNotFound { "TestSuite (id=$id) not found" }

    /**
     * @param source source of the test suite
     * @param version version of snapshot of source
     * @return matched test suites
     */
    fun getBySourceAndVersion(
        source: TestSuitesSource,
        version: String
    ): List<TestSuite> = testSuiteRepository.findAllBySourceAndVersion(source, version)

    /**
     * Delete testSuites and related tests & test executions from DB
     *
     * @param testSuiteDtos suites, which need to be deleted
     */
    @Suppress("UnsafeCallOnNullableType")
    fun deleteTestSuiteDto(testSuiteDtos: List<TestSuiteDto>) {
        testSuiteDtos.forEach { testSuiteDto ->
            // Get test suite id by testSuiteDto
            val testSuiteId = getSavedIdByDto(testSuiteDto)

            // Get test ids related to the current testSuiteId
            val testIds = testRepository.findAllByTestSuiteId(testSuiteId).map { it.requiredId() }
            testIds.forEach { testId ->
                // Executions could be absent
                testExecutionRepository.findByTestId(testId).ifPresent { testExecution ->
                    // Delete test executions
                    val testExecutionId = testExecution.requiredId()
                    log.debug { "Delete test execution with id $testExecutionId" }
                    testExecutionRepository.deleteById(testExecutionId)
                }
                // Delete tests
                log.debug { "Delete test with id $testId" }
                testRepository.deleteById(testId)
            }
            log.info("Delete test suite ${testSuiteDto.name} with id $testSuiteId")
            testSuiteRepository.deleteById(testSuiteId)
        }
    }

    private fun getSavedIdByDto(
        dto: TestSuiteDto,
    ): Long = testSuiteRepository.findByNameAndSourceAndVersion(
        dto.name,
        testSuitesSourceService.getByName(dto.source.organizationName, dto.source.name),
        dto.version
    )
        ?.requiredId()
        .orNotFound { "TestSuite (name=${dto.name} in ${dto.source.name} with version ${dto.version}) not found" }

    companion object {
        private val log = LoggerFactory.getLogger(TestSuitesService::class.java)
    }
}
