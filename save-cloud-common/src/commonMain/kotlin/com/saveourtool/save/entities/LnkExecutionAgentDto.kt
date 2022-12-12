package com.saveourtool.save.entities

import com.saveourtool.save.execution.ExecutionDto
import kotlinx.serialization.Serializable

/**
 * @property execution
 * @property agent
 */
@Serializable
data class LnkExecutionAgentDto(
    val execution: ExecutionDto,
    val agent: AgentDto,
)
