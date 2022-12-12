package com.saveourtool.save.orchestrator.runner

import com.saveourtool.save.orchestrator.service.ContainerService
import kotlin.jvm.Throws

internal const val SAVE_AGENT_USER_HOME = "/home/save-agent"
internal const val EXECUTION_DIR = "$SAVE_AGENT_USER_HOME/save-execution"

/**
 * Describes operations that should be supported with a specific engine for running save-agents.
 */
interface ContainerRunner {
    /**
     * Create a [replicas] number of agents for an execution with id [executionId].
     *
     * @param executionId and ID of execution for which agents will run tests
     * @param configuration [ContainerService.RunConfiguration] for the created containers
     * @param replicas number of agents acting in parallel
     * @throws ContainerRunnerException when runner fails to create or start containers
     */
    @Throws(ContainerRunnerException::class)
    fun createAndStart(
        executionId: Long,
        configuration: ContainerService.RunConfiguration,
        replicas: Int,
    )

    /**
     * Stop all agents in an execution. Currently, not used.
     * TODO: actually call
     *
     * @param executionId
     */
    fun stop(executionId: Long)

    /**
     * @param containerId ID of container that should be stopped
     * @return true if agent has been stopped successfully
     * todo: distinguish stopped / not stopped / error / already stopped
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun stopByContainerId(containerId: String): Boolean

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
     * @param executionId
     * @return list of container id which are run for [executionId]
     */
    fun listContainerIds(executionId: Long): List<String>

    /**
     * Check whether the agent [containerId] is stopped
     *
     * @param containerId id of the agent
     * @return true if agent is not running
     */
    fun isStoppedByContainerId(containerId: String): Boolean

    /**
     * Get container identifier: container name for docker agent runner and container id for kubernetes
     *
     * @param containerId
     * @return container identifier
     */
    fun getContainerIdentifier(containerId: String): String
}
