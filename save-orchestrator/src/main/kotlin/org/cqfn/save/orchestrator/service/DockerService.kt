package org.cqfn.save.orchestrator.service

import org.cqfn.save.domain.RunConfiguration
import org.cqfn.save.entities.Execution
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.orchestrator.config.ConfigProperties
import org.cqfn.save.orchestrator.docker.ContainerManager
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText

@Service
@OptIn(ExperimentalPathApi::class)
class DockerService(private val configProperties: ConfigProperties) {
    private val containerManager = ContainerManager(configProperties.docker.host)

    /**
     * Function that builds a base image with test resources and then creates containers with agents.
     */
    fun buildAndCreateContainer(execution: Execution) {
        val imageId = buildBaseImageForExecution(execution)
        createContainerForExecution(imageId)
    }

    private fun buildBaseImageForExecution(execution: Execution): String {
        val resourcesPath = File(
            configProperties.testResources.basePath,
            execution.resourcesRootPath
        )
        val imageId = containerManager.buildImageWithResources(
            baseDir = resourcesPath,
            resourcesPath = "/save-agent"
        )
        return imageId
    }

    private fun createContainerForExecution(imageId: String) {
        val containerId = containerManager.createContainerFromImage(
            imageId,
            RunConfiguration("./save-agent", "save-agent"),
            "todo",
        )
        val agentPropertiesFile = createTempFile("agent")
        agentPropertiesFile.writeText(
            """
                agent.id=$containerId
            """.trimIndent()
        )
        containerManager.copyResourcesIntoContainer(
            containerId, "/run",
            listOf(ClassPathResource("save-agent").file, agentPropertiesFile.toFile())
        )
    }
}
