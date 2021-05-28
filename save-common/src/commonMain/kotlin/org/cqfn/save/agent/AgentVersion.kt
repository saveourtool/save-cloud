package org.cqfn.save.agent

import kotlinx.serialization.Serializable

/**
 * @property containerId agent id
 * @property version agent version
 */
@Serializable
data class AgentVersion(
    val containerId: String,
    val version: String,
)
