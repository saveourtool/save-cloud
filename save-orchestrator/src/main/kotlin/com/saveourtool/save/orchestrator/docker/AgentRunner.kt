package com.saveourtool.save.orchestrator.docker

/**
 * Describes operations that should be supported with a specific engine for running save-agents.
 */
interface AgentRunner {
    /**
     * Create a [replicas] number of agents for an execution with id [executionId].
     *
     * @param baseImageId an ID of docker image that will be used as a base for agents
     * @param agentRunCmd a command that should be container's entrypoint (see docker's CMD directive)
     * @param executionId and ID of execution for which agents will run tests
     * @param replicas number of agents acting in parallel
     * @param workingDir target execution directory
     * @return unique identifier of created instances that can be used to manipulate them later
     */
    fun create(
        executionId: Long,
        baseImageId: String,
        replicas: Int,
        workingDir: String,
        agentRunCmd: String,
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
     * @param agentId
     */
    fun stopByAgentId(agentId: String): Boolean

    /**
     * @param executionId
     */
    fun cleanup(executionId: Long)

    /**
     * Base on id of an execution load data about existing running agents for it.
     * TODO: implement under https://github.com/saveourtool/save-cloud/issues/11
     *
     * @param executionId
     */
    fun discover(executionId: Long) {
        TODO("Not yet implemented")
    }
}
