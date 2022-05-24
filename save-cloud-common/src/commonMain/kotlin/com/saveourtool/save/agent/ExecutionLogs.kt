package com.saveourtool.save.agent

import kotlinx.serialization.Serializable

/**
 * @property agentId unique ID of the agent
 * @property cliLogs
 */
@Serializable
data class ExecutionLogs(
    val agentId: String,
    val cliLogs: List<String>
)
