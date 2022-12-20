package com.saveourtool.save.orchestrator.runner

import com.saveourtool.save.orchestrator.service.ContainerService

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
     * @return unique identifier of created instances that can be used to manipulate them later
     */
    fun create(
        executionId: Long,
        configuration: ContainerService.RunConfiguration,
        replicas: Int,
    ): List<String>

    /**
     * @param executionId
     */
    fun startAllByExecution(executionId: Long)

    /**
     * @param executionId
     */
    fun cleanupAllByExecution(executionId: Long)

    /**
     * Check whether the agent [containerId] is stopped
     *
     * @param containerId id of the agent
     * @return true if agent is not running
     */
    fun isStopped(containerId: String): Boolean

    /**
     * Get container identifier: container name for docker agent runner and container id for kubernetes
     *
     * @param containerId
     * @return container identifier
     */
    fun getContainerIdentifier(containerId: String): String

    /**
     * [ContainerRunner] which implements this interface allows to stop containers
     */
    interface Stoppable {
        /**
         * @param containerId ID of container that should be stopped
         * @return true if agent has been stopped successfully
         * todo: distinguish stopped / not stopped / error / already stopped
         */
        @Suppress("FUNCTION_BOOLEAN_PREFIX")
        fun stop(containerId: String): Boolean
    }

    /**
     * [ContainerRunner] which implements this interface requires prune old data
     */
    interface Prunable {
        /**
         * Prune old docker data
         */
        fun prune()
    }
}
