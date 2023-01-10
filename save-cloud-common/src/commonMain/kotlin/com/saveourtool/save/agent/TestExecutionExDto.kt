package com.saveourtool.save.agent

import com.saveourtool.save.test.analysis.metrics.NoDataAvailable
import com.saveourtool.save.test.analysis.metrics.TestMetrics
import com.saveourtool.save.test.analysis.results.AnalysisResult
import kotlinx.serialization.Serializable

/**
 * An "extended" [test execution][TestExecutionDto] with some extra information,
 * such as test metrics.
 *
 * Use [TestExecutionDto.extended] to create an extension.
 *
 * @property testExecution the actual test execution data.
 * @property testMetrics scalar test metrics.
 * @property analysisResults test analysis results.
 * @see TestExecutionDto
 */
@Serializable
data class TestExecutionExDto(
    val testExecution: TestExecutionDto,
    val testMetrics: TestMetrics = NoDataAvailable.instance,
    val analysisResults: List<AnalysisResult> = emptyList(),
)
