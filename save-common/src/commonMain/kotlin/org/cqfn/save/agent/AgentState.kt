package org.cqfn.save.agent

import kotlinx.serialization.Serializable

/**
 * Possible states of the Agent
 */
@Serializable
enum class AgentState {
    IDLE, BUSY, FINISHED, ERROR
}
