package org.cqfn.save.orchestrator.docker

interface AgentRunner {
    /**
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

    fun start(id: String)

    /**
     * Stop all agents in an execution. Currently, not used.
     */
    fun stop(executionId: String)

    fun stopByAgentId(agentId: String)

    fun cleanup(executionId: Long)
}
