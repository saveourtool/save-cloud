@file:Suppress("FILE_UNORDERED_IMPORTS")

package com.saveourtool.save.backend.service

import com.saveourtool.save.test.analysis.api.TestId
import com.saveourtool.save.test.analysis.api.TestStatisticsStorage
import com.saveourtool.save.test.analysis.internal.MemoryBacked
import com.saveourtool.save.test.analysis.metrics.TestMetrics
import com.saveourtool.save.test.analysis.results.AnalysisResult
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import com.saveourtool.save.test.analysis.api.TestAnalysisService as LowLevelAnalysisService

/**
 * The high-level test analysis service.
 *
 * @see LowLevelAnalysisService
 * @see TestStatisticsStorage
 */
@Service
class TestAnalysisService {
    private val statisticsStorage: TestStatisticsStorage = MemoryBacked(slidingWindowSize = Int.MAX_VALUE)
    private val lowLevelAnalysisService = LowLevelAnalysisService(statisticsStorage)

    /**
     * Returns the aggregate statistics for the given [testId].
     *
     * @param testId the unique test id.
     * @return the scalar test metrics for the given [testId].
     * @see TestStatisticsStorage.getTestMetrics
     */
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
}
