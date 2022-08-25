package com.saveourtool.save.orchestrator.runner

import com.saveourtool.save.orchestrator.service.DockerService
import com.saveourtool.save.orchestrator.service.PersistentVolumeId

internal const val SAVE_AGENT_USER_HOME = "/home/save-agent"
internal const val EXECUTION_DIR = "$SAVE_AGENT_USER_HOME/save-execution"

/**
 * Describes operations that should be supported with a specific engine for running save-agents.
 */
interface AgentRunner {
    /**
     * Create a [replicas] number of agents for an execution with id [executionId].
     *
     * @param executionId and ID of execution for which agents will run tests
     * @param configuration [DockerService.RunConfiguration] for the created containers
     * @param replicas number of agents acting in parallel
     * @return unique identifier of created instances that can be used to manipulate them later
     */
    fun create(
        executionId: Long,
        configuration: DockerService.RunConfiguration<PersistentVolumeId>,
        replicas: Int,
    ): List<String>

    /**
     * @param executionId
     */
    fun start(executionId: Long)

    /**
     * Stop all agents in an execution. Currently, not used.
     * TODO: actually call
     *
     * @param executionId
     */
    fun stop(executionId: Long)

    /**
     * @param agentId ID of agent that should be stopped
     * @return true if agent has been stopped successfully
     * todo: distinguish stopped / not stopped / error / already stopped
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun stopByAgentId(agentId: String): Boolean

    /**
     * @param executionId
     */
    fun cleanup(executionId: Long)

    /**
     * Prune old docker data
     */
    fun prune()

    /**
     * Base on id of an execution load data about existing running agents for it.
     * TODO: implement under https://github.com/saveourtool/save-cloud/issues/11
     *
     * @param executionId
     */
    fun discover(executionId: Long) {
        TODO("Not yet implemented")
    }

    /**
     * Check whether the agent [agentId] is stopped
     *
     * @param agentId id of the agent
     * @return true if agent is not running
     */
    fun isAgentStopped(agentId: String): Boolean
}
