package com.saveourtool.save.agent

import com.saveourtool.save.domain.TestResultStatus

import kotlinx.serialization.Serializable

/**
 * @property agentContainerId
 * @property status
 * @property startTimeSeconds
 * @property endTimeSeconds
 * @property filePath
 * @property pluginName name of a plugin which will execute test at [filePath]
 * @property testSuiteName a name of test suite, a test from which has been executed
 * @property tags list of tags of current test
 * @property missingWarnings missing warnings
 * @property matchedWarnings matched warnings
 * @property hasDebugInfo whether debug info data is available for this test execution
 */
@Serializable
data class TestExecutionDto(
    val filePath: String,
    val pluginName: String,
    val agentContainerId: String?,
    val status: TestResultStatus,
    val startTimeSeconds: Long?,
    val endTimeSeconds: Long?,
    val testSuiteName: String? = null,
    val tags: List<String> = emptyList(),
    val missingWarnings: Int?,
    val matchedWarnings: Int?,
    val hasDebugInfo: Boolean? = null,
)
