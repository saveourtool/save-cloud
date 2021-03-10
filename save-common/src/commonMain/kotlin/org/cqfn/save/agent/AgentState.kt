package org.cqfn.save.agent

import kotlinx.serialization.Serializable

/**
 * Possible states of the Agent
 */
@Serializable
enum class AgentState {
    /**
     * Backend returned non-OK code
     */
    BACKEND_FAILURE,

    /**
     * Backend returned no code at all
     */
    BACKEND_UNREACHABLE,

    /**
     * Agent is doing work
     */
    BUSY,

    /**
     * SAVE CLI failed
     */
    CLI_FAILED,

    /**
     * Agent has finished execution
     */
    FINISHED,

    /**
     * Agent is doing nothing
     */
    IDLE,
    ;
}
