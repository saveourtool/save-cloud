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
     * [ContainerRunner] which implements this interface requires prune old data
     */
    interface Prunable {
        /**
         * Prune old docker data
         */
        fun prune()
    }
}
