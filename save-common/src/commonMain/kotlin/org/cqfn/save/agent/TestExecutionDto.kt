package org.cqfn.save.agent

import kotlinx.serialization.Serializable
import org.cqfn.save.domain.TestResultStatus

@Serializable
data class TestExecutionDto(
    val id: Long,
    var agentId: Long,
    var status: TestResultStatus,
    var startTime: Long,
    var endTime: Long,
)