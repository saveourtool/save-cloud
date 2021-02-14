/**
 * Model classes for heartbeating between save agent and the orchestrator
 */

package org.cqfn.save.agent

import kotlinx.serialization.Serializable

/**
 * A heartbeat sent from Agent to Orchestrator.
 *
 * @property state current state of the Agent
 * @property percentCompletion percentage of completed jobs, integer 0..100
 */
@Serializable
data class Heartbeat(val state: AgentState, val percentCompletion: Int) {
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
