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
    var agentId: Long,
    var status: TestResultStatus,
    var startTime: Long,
    var endTime: Long,
)
