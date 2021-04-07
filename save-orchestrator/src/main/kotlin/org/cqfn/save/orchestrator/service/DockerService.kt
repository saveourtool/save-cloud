package org.cqfn.save.orchestrator.service

import org.cqfn.save.entities.Execution
import org.cqfn.save.orchestrator.config.ConfigProperties
import org.cqfn.save.orchestrator.docker.ContainerManager
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText

/**
 * A service that uses [ContainerManager] to build and start containers for test execution.
 */
@Service
@OptIn(ExperimentalPathApi::class)
class DockerService(private val configProperties: ConfigProperties) {
    /**
     * [ContainerManager] that is used to access docker daemon API
     */
    internal val containerManager = ContainerManager(configProperties.docker.host)
    private val executionDir = "/run/save-execution"

    /**
     * Function that builds a base image with test resources and then creates containers with agents.
     *
     * @param execution [Execution] from which this workflow is started
     * @return list of IDs of created containers
     */
    fun buildAndCreateContainers(execution: Execution): List<String> {
        val imageId = buildBaseImageForExecution(execution)
        return (0 until configProperties.agentsCount).map { number ->
            createContainerForExecution(imageId, "${execution.id}-$number")
        }
    }

    private fun buildBaseImageForExecution(execution: Execution): String {
        val resourcesPath = File(
            configProperties.testResources.basePath,
            execution.resourcesRootPath,
        )
        // include save-agent into the image
        ClassPathResource(SAVE_AGENT_EXECUTABLE_NAME).file.copyTo(File(resourcesPath, SAVE_AGENT_EXECUTABLE_NAME))
        val imageId = containerManager.buildImageWithResources(
            imageName = "save-execution:${execution.id}",
            baseDir = resourcesPath,
            resourcesPath = executionDir,
            runCmd = """RUN apt-get update && apt-get install -y libcurl4-openssl-dev && rm -rf /var/lib/apt/lists/*
                    |RUN chmod +x $executionDir/$SAVE_AGENT_EXECUTABLE_NAME
                """
        )
        return imageId
    }

    private fun createContainerForExecution(imageId: String, containerNumber: String): String {
        val containerId = containerManager.createContainerFromImage(
            imageId,
            listOf("$executionDir/$SAVE_AGENT_EXECUTABLE_NAME"),
            "save-execution-$containerNumber",
        )
        val agentPropertiesFile = createTempFile("agent")
        agentPropertiesFile.writeText(
            """
                agent.id=$containerId
            """.trimIndent()
        )
        containerManager.copyResourcesIntoContainer(
            containerId, executionDir,
            listOf(agentPropertiesFile.toFile())
        )
        return containerId
    }

    companion object {
        private const val SAVE_AGENT_EXECUTABLE_NAME = "save-agent.kexe"
    }
}
