package org.cqfn.save.agent

/**
 * A heartbeat sent from Agent to Orchestrator.
 *
 * @property state current state of the Agent
 * @property percentCompletion percentage of completed jobs, integer 0..100
 */
data class Heartbeat(val state: AgentState, val percentCompletion: Int)
