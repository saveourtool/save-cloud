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
     * Agent has been crashed
     */
    CRASHED,

    /**
     * Agent has finished execution
     */
    FINISHED,

    /**
     * Agent is doing nothing
     */
    IDLE,

    /**
     * Agent has just started and hasn't received any heartbeats yet
     */
    STARTING,

    /**
     * Agent has been stopped by save-orchestrator, because there is no more work left
     */
    STOPPED_BY_ORCH,
    ;
}
