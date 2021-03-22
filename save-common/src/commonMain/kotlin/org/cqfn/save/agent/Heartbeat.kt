/**
 * Model classes for heartbeating between save agent and the orchestrator
 */

package org.cqfn.save.agent

import kotlinx.serialization.Serializable
import org.cqfn.save.test.TestDto

/**
 * Progress of tests execution
 *
 * @property percentCompletion percentage of completed jobs, integer 0..100
 */
@Serializable
data class ExecutionProgress(val percentCompletion: Int) {
    init {
        require(percentCompletion in 0..100) { "percentCompletion should be in 0..100, but is $percentCompletion" }
    }
}

/**
 * A heartbeat sent from Agent to Orchestrator.
 *
 * @property agentId unique ID of the agent which sent the heartbeat
 * @property state current state of the Agent
 * @property executionProgress current progress of tests execution with this Agent
 */
@Serializable
data class Heartbeat(val agentId: String,
                     val state: AgentState,
                     val executionProgress: ExecutionProgress)

/**
 * A response from Orchestrator to Agent
 */
@Serializable sealed class HeartbeatResponse
@Serializable object WaitResponse : HeartbeatResponse()
@Serializable object ContinueResponse : HeartbeatResponse()

/**
 * @property ids a list of new jobs for this agent
 */
@Serializable data class NewJobResponse(val ids: List<TestDto>) : HeartbeatResponse()
