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
