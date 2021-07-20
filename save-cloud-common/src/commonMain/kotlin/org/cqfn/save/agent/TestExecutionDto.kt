package org.cqfn.save.agent

import org.cqfn.save.domain.TestResultStatus

import kotlinx.serialization.Serializable

/**
 * @property agentContainerId
 * @property status
 * @property startTimeSeconds
 * @property endTimeSeconds
 * @property filePath
 * @property pluginName name of a plugin which will execute test at [filePath]
 */
@Serializable
data class TestExecutionDto(
    val filePath: String,
    val pluginName: String,
    val agentContainerId: String?,
    val status: TestResultStatus,
    val startTimeSeconds: Long?,
    val endTimeSeconds: Long?,
)
