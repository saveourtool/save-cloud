package com.saveourtool.save.orchestrator.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.BuildImageResultCallback
import io.micrometer.core.instrument.MeterRegistry
import org.cqfn.save.domain.Sdk
import org.cqfn.save.orchestrator.copyRecursivelyWithAttributes
import org.cqfn.save.orchestrator.execTimed
import org.cqfn.save.orchestrator.getHostIp
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import java.io.File

import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile

/**
 * A class that communicates with docker daemon
 */
@Component
class DockerContainerManager(
    private val meterRegistry: MeterRegistry,
    private val dockerClient: DockerClient,
) {
    /**
     * Creates a docker image with provided [resources]
     *
     * @param baseImage base docker image from which this image will be built
     * @param baseDir a context dir for Dockerfile
     * @param resourcesPath target path to additional resources. Resources from baseDir will be copied into this directory inside of the container.
     * @param runCmd command to append to the Dockerfile. Actual entrypoint is added on container creation.
     * @param imageName name which will be assigned to the image
     * @return id of the created docker image
     * @throws DockerException
     */
    @Suppress("TOO_LONG_FUNCTION", "LongMethod")
    internal fun buildImageWithResources(baseImage: String = Sdk.Default.toString(),
                                         imageName: String,
                                         baseDir: File,
                                         resourcesPath: String,
                                         runCmd: String = "RUN /bin/bash",
    ): String {
        val tmpDir = createTempDirectory().toFile()
        val tmpResourcesDir = tmpDir.absoluteFile.resolve("resources")
        log.debug("Copying ${baseDir.absolutePath} into $tmpResourcesDir")
        copyRecursivelyWithAttributes(baseDir, tmpResourcesDir)
        val dockerFileAsText =
                """
                    |FROM $baseImage
                    |COPY resources $resourcesPath
                    |$runCmd
                """.trimMargin()
        val dockerFile = createTempFile(tmpDir.toPath()).toFile()
        dockerFile.writeText(dockerFileAsText)
        val hostIp = getHostIp()
        log.debug("Resolved host IP as $hostIp, will add it to the container")
        val buildImageResultCallback: BuildImageResultCallback = try {
            val buildCmd = dockerClient.buildImageCmd(dockerFile)
                .withBaseDirectory(tmpDir)
                .withTags(setOf(imageName))
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

    companion object {
        private val log = LoggerFactory.getLogger(DockerContainerManager::class.java)
    }
}
