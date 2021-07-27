/**
 * Model classes for heartbeating between save agent and the orchestrator
 */

package org.cqfn.save.agent

import org.cqfn.save.test.TestDto

import kotlinx.serialization.Serializable

/**
 * Progress of tests execution
 *
 * @property percentCompletion percentage of completed jobs, integer 0..100
 */
@Serializable
data class ExecutionProgress(val percentCompletion: Int) {
    init {
        @Suppress("MAGIC_NUMBER", "MagicNumber")
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

// fixme: there are some complications when using serializable objects on Native
// https://github.com/Kotlin/kotlinx.serialization/issues/1616
@Serializable
@Suppress("CanSealedSubClassBeObject")
class WaitResponse : HeartbeatResponse()

@Serializable
@Suppress("CanSealedSubClassBeObject")
class ContinueResponse : HeartbeatResponse()

/**
 * @property tests a list of new jobs for this agent
 * @property cliArgs command line arguments for SAVE launch
 */
@Serializable data class NewJobResponse(val tests: List<TestDto>, val cliArgs: String) : HeartbeatResponse()
