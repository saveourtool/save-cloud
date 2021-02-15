package org.cqfn.save.orchestrator.model

import kotlinx.serialization.Serializable

@Serializable
data class Heartbeat(val agentId: String,
                     val state: AgentState,
                     val percentCompletion: Int) {
    init {
        require(percentCompletion in 0..100) { "percentCompletion should be in 0..100, but is $percentCompletion" }
    }
}

/**
 * A response from Orchestrator to Agent
 */
@Serializable sealed class HeartbeatResponse
@Serializable object EmptyResponse : HeartbeatResponse()

/**
 * @property ids a list of new jobs for this agent
 */
@Serializable data class NewJobResponse(val ids: List<String>) : HeartbeatResponse()
@Serializable object TerminatingResponse : HeartbeatResponse()
