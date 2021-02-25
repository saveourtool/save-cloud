package org.cqfn.save.agent

import kotlinx.serialization.Serializable

/**
 * Possible states of the Agent
 */
@Serializable
enum class AgentState {
    BUSY,
    CLI_FAILED,
    BACKEND_FAILURE,
    BACKEND_UNREACHABLE,
    FINISHED,
    IDLE,
    ;
}
