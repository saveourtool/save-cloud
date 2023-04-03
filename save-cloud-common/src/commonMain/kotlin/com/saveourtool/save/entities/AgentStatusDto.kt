package com.saveourtool.save.entities

import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.utils.getCurrentLocalDateTime
import kotlinx.datetime.LocalDateTime

typealias AgentStatusDtoList = List<AgentStatusDto>

/**
 * @property state current state of the agent
 * @property containerId id of the agent's container
 * @property time
 */
data class AgentStatusDto(
    val state: AgentState,
    val containerId: String,
    val time: LocalDateTime = getCurrentLocalDateTime(),
)
