package org.cqfn.save.agent

import org.cqfn.save.domain.TestResultStatus

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * @property agentContainerId
 * @property status
 * @property startTime
 * @property endTime
 * @property filePath
 */
@Serializable
data class TestExecutionDto(
    val filePath: String,
    val agentContainerId: String?,
    val status: TestResultStatus,
    val startTime: Instant?,
    val endTime: Instant?,
)
