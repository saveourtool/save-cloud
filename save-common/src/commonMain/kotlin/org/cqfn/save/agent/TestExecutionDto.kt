package org.cqfn.save.agent

import org.cqfn.save.domain.TestResultStatus

import kotlinx.serialization.Serializable

/**
 * @property id
 * @property agentId
 * @property status
 * @property startTimeSeconds
 * @property endTimeSeconds// todo: since datetime 0.2.0 can use serializable Instant
 * @property filePath
 */
@Serializable
data class TestExecutionDto(
    val id: Long,
    val filePath: String,
    val agentId: Long?,
    val status: TestResultStatus,
    val startTimeSeconds: Long?,
    val endTimeSeconds: Long?,
)
