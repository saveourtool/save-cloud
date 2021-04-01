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
    internal val containerManager = ContainerManager(configProperties.docker.host)
    private val executionDir = "/run/save-execution"

    /**
     * Function that builds a base image with test resources and then creates containers with agents.
     */
    fun buildAndCreateContainer(execution: Execution): String {
        val imageId = buildBaseImageForExecution(execution)
        return createContainerForExecution(imageId, "${execution.id}-1")
    }

    private fun buildBaseImageForExecution(execution: Execution): String {
        val resourcesPath = File(
            configProperties.testResources.basePath,
            execution.resourcesRootPath,
        )
        val imageId = containerManager.buildImageWithResources(
            imageName = "save-execution:${execution.id}",
            baseDir = resourcesPath,
            resourcesPath = executionDir,
        )
        return imageId
    }

    private fun createContainerForExecution(imageId: String, containerNumber: String): String {
        // fixme: there is no sense to support windows executable
        val agentExecutableExtension = if (System.getProperty("os.name").startsWith("windows", ignoreCase = true)) "exe" else "kexe"
        val containerId = containerManager.createContainerFromImage(
            imageId,
            RunConfiguration(listOf("$executionDir/save-agent.$agentExecutableExtension"), "save-agent"),
            "save-execution-$containerNumber",
        )
        val agentPropertiesFile = createTempFile("agent")
        agentPropertiesFile.writeText(
            """
                agent.id=$containerId
            """.trimIndent()
        )
        // todo: check if executable is copied with execution permissions
        containerManager.copyResourcesIntoContainer(
            containerId, executionDir,
            listOf(ClassPathResource("save-agent.$agentExecutableExtension").file, agentPropertiesFile.toFile())
        )
        return containerId
    }
}
