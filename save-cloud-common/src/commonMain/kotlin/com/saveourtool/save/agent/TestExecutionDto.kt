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
 * @property unmatched number of unmatched checks/validations in test (false negative results)
 * @property matched number of matched checks/validations in test (true positive results)
 * @property expected number of all checks/validations in test (unmatched + matched)
 * @property unexpected number of matched,but not expected checks/validations in test (false positive results)
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
    val unmatched: Long?,
    val matched: Long?,
    val expected: Long?,
    val unexpected: Long?,
    val hasDebugInfo: Boolean? = null,
    val executionId: Long? = null
)
