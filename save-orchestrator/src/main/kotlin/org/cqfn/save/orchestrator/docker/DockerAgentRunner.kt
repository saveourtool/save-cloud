package org.cqfn.save.orchestrator.docker

import com.github.dockerjava.api.DockerClient
import org.cqfn.save.orchestrator.service.DockerService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!kubernetes")
class DockerAgentRunner(
    private val dockerClient: DockerClient,
) : AgentRunner {
    override fun create(
        baseImageId: String,
        replicas: Int,
        workingDir: String,
        runCmd: String,
        containerName: String
    ): String? {
        (1..replicas).map { number ->
            logger.info("Building container #$number for execution.id=${execution.id}")
            createContainerForExecution(execution, imageId, "${execution.id}-$number", runCmd, saveCliExecFlags).also {
                logger.info("Built container id=$it for execution.id=${execution.id}")
            }
        }
    }

    override fun start(id: String) {
        dockerClient.startContainerCmd(id).exec()
    }

    override fun stop(id: String) {
        dockerClient.stopContainerCmd(id).exec()
    }

    companion object {
        private val logger =
    }
}