package com.saveourtool.save.entities

import kotlinx.serialization.Serializable

/**
 * @property containerId id of the container, inside which the agent is running
 * @property executionId id of the execution, which the agent is serving
 * @property version version of save-cli [generated.SAVE_CLOUD_VERSION]
 */
@Serializable
data class AgentDto(
    val containerId: String,
    val executionId: Long,
    val version: String? = null,
)
