package com.saveourtool.save.orchestrator.docker

import com.saveourtool.save.domain.Sdk
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.copyRecursivelyWithAttributes
import com.saveourtool.save.orchestrator.execTimed
import com.saveourtool.save.orchestrator.getHostIp
import com.saveourtool.save.orchestrator.service.isBaseImageName

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.BuildImageResultCallback
import com.github.dockerjava.api.model.Image
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import java.io.File

import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile

/**
 * A class that communicates with docker daemon
 */
@Component
@Suppress("AVOID_NULL_CHECKS")
class DockerContainerManager(
    private val configProperties: ConfigProperties,
    private val meterRegistry: MeterRegistry,
    private val dockerClient: DockerClient,
) {
    /**
     * Creates a docker image with provided [resources]
     *
     * @param baseImage base docker image from which this image will be built
     * @param baseDir a context dir for Dockerfile
     * @param resourcesTargetPath target path to additional resources. Resources from [baseDir] will be copied into this directory inside the container.
     * @param imageName name which will be assigned to the image
     * @param runCmd `RUN` directives to be added to Dockerfile *before* resources from `baseDir` are copied (so that resources-agnostic command
     * results can be cached in docker layers).
     * @param runOnResourcesCmd `RUN` directives to be added to Dockerfile *after* resources from `baseDir` are copied.
     * @return id of the created docker image
     * @throws DockerException
     */
    @Suppress(
        "TOO_LONG_FUNCTION",
        "LongMethod",
        "LongParameterList",
        "TOO_MANY_PARAMETERS"
    )
    internal fun buildImageWithResources(baseImage: String = Sdk.Default.toString(),
                                         imageName: String,
                                         baseDir: File?,
                                         resourcesTargetPath: String?,
                                         runCmd: String = "RUN /bin/bash",
                                         runOnResourcesCmd: String? = null,
    ): String {
        val tmpDir = createTempDirectory().toFile()
        val tmpResourcesDir = tmpDir.absoluteFile.resolve("resources")
        if (baseDir != null) {
            log.debug("Copying ${baseDir.absolutePath} into $tmpResourcesDir")
            copyRecursivelyWithAttributes(baseDir, tmpResourcesDir)
        }
        val dockerFile = createDockerFile(tmpDir, baseImage, resourcesTargetPath, runCmd, runOnResourcesCmd)
        val hostIp = getHostIp()
        log.debug("Resolved host IP as $hostIp, will add it to the container")
        val buildImageResultCallback: BuildImageResultCallback = try {
            val buildCmd = dockerClient.buildImageCmd(dockerFile)
                .withBaseDirectory(tmpDir)
                .withTags(setOf(imageName))
                .withLabels(mapOf("save-id" to imageName))
                // this is required to be able to access host, e.g. if proxy running on the host is required during image build process
                .withExtraHosts(setOf("host.docker.internal:$hostIp"))
            buildCmd.execTimed(meterRegistry, "save.orchestrator.docker.build", "baseImage", baseImage) { record ->
                object : BuildImageResultCallback() {
                    override fun onComplete() {
                        super.onComplete()
                        record()
                    }
                }
            }
        } finally {
            dockerFile.delete()
            tmpDir.deleteRecursively()
        }
        return buildImageResultCallback.awaitImageId()
    }

    /**
     * Returns all images labelled `save-id=[saveId]`
     *
     * @param saveId value of `save-id` label to search by
     * @return a list of images with matching label
     */
    internal fun findImages(saveId: String): List<Image> = dockerClient.listImagesCmd()
        // Can't use filters on the daemon level: https://github.com/docker-java/docker-java/issues/1517
        // .withImageNameFilter("label=\"save-id=$imageName\"")
        .exec()
        .filter { it.labels?.get("save-id") == saveId }

    private fun createDockerFile(
        dir: File,
        baseImage: String,
        resourcesPath: String?,
        runCmd: String,
        runOnResourcesCmd: String? = null,
    ): File {
        val dockerFileAsText = buildString {
            appendLine("FROM ${configProperties.docker.registry}/$baseImage")
            appendLine(runCmd)
            appendLine("RUN useradd --create-home --shell /bin/sh save-agent")
            appendLine("WORKDIR /home/save-agent/save-execution")
            if (resourcesPath != null) {
                appendLine("COPY resources $resourcesPath")
                runOnResourcesCmd?.let(::appendLine)
            }
            if (isBaseImageName(baseImage)) {
                // If image is being built from base image for SAVE, then we can assume that it is an image for execution.
                // Then we can finalize build process by switching to an unprivileged user.
                appendLine("RUN chown -R save-agent .")
                appendLine("USER save-agent")
            }
        }
        log.debug("Using generated Dockerfile {}", dockerFileAsText)
        val dockerFile = createTempFile(dir.toPath()).toFile()
        dockerFile.writeText(dockerFileAsText)
        return dockerFile
    }

    companion object {
        private val log = LoggerFactory.getLogger(DockerContainerManager::class.java)
    }
}
