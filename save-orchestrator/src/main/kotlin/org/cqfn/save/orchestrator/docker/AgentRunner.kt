package org.cqfn.save.orchestrator.docker

interface AgentRunner {
    /**
     * Create a [replicas] number of agents for an execution with id [executionId].
     *
     * @param baseImageId an ID of docker image that will be used as a base for agents
     * @param agentRunCmd a command that should be container's entrypoint (see docker's CMD directive)
     * @return unique identifier of created instances that can be used to manipulate them later
     */
    fun create(
        executionId: Long,
        baseImageId: String,
        replicas: Int,
        workingDir: String,
        agentRunCmd: String,
    ): List<String>

    fun start(executionId: Long)

    /**
     * Stop all agents in an execution. Currently, not used.
     */
    fun stop(executionId: String)

    fun stopByAgentId(agentId: String)

    fun cleanup(executionId: Long)

    /**
     * Base on id of an execution load data about existing running agents for it.
     */
    fun discover(executionId: Long) {
        TODO("Not yet implemented")
    }
}
