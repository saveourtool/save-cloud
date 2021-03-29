package org.cqfn.save.agent

import org.cqfn.save.domain.TestResultStatus

import kotlinx.serialization.Serializable

/**
 * @property id
 * @property agentId
 * @property status
 * @property startTime
 * @property endTime
 */
@Serializable
data class TestExecutionDto(
    val id: Long,
    val agentId: Long,
    val status: TestResultStatus,
    val startTime: Long,
    val endTime: Long,
)
