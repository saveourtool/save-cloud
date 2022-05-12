package org.cqfn.save.orchestrator.docker

interface AgentRunner {
    /**
     * @return unique identifier of the created instance that can be used to manipulate it later
     */
    fun create(baseImageId: String,
               replicas: Int,
               workingDir: String,
               runCmd: String,
               containerName: String,
    ): String?

    fun start(id: String)

    fun stop(id: String)
}