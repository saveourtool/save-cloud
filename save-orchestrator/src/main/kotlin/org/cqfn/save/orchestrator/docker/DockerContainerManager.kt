package org.cqfn.save.orchestrator.docker

import org.cqfn.save.domain.Sdk
import org.cqfn.save.orchestrator.DOCKER_METRIC_PREFIX
import org.cqfn.save.orchestrator.config.DockerSettings
import org.cqfn.save.orchestrator.copyRecursivelyWithAttributes
import org.cqfn.save.orchestrator.execTimed
import org.cqfn.save.orchestrator.getHostIp

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.BuildImageResultCallback
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.LogConfig
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import io.micrometer.core.instrument.MeterRegistry
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.cqfn.save.orchestrator.config.ConfigProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.GZIPOutputStream

import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText

/**
 * A class that communicates with docker daemon
 *
 * @property settings setting of docker daemon
 */
@Component
class DockerContainerManager(
    configProperties: ConfigProperties,
    private val meterRegistry: MeterRegistry,
    private val dockerClient: DockerClient,
) {
    private val settings: DockerSettings = configProperties.docker

    /**
     * Creates a docker container
     *
     * @param runCmd an entrypoint for docker container with CLI arguments
     * @param containerName a name for the created container
     * @param baseImageId id of the base docker image for this container
     * @param workingDir working directory for [runCmd]
     * @return id of created container or null if it wasn't created
     * @throws DockerException if docker daemon has returned an error
     * @throws RuntimeException if an exception not specific to docker has occurred
     */
    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
    internal fun createContainerFromImage(baseImageId: String,
                                          workingDir: String,
                                          runCmd: String,
                                          containerName: String,
    ): String {
        val baseImage = dockerClient.listImagesCmd().execTimed(meterRegistry, "$DOCKER_METRIC_PREFIX.image.list")!!.find {
            // fixme: sometimes createImageCmd returns short id without prefix, sometimes full and with prefix.
            it.id.replaceFirst("sha256:", "").startsWith(baseImageId.replaceFirst("sha256:", ""))
        }
            ?: error("Image with requested baseImageId=$baseImageId is not present in the system")
        // createContainerCmd accepts image name, not id, so we retrieve it from tags
        val createContainerCmdResponse = dockerClient.createContainerCmd(baseImage.repoTags.first())
            .withWorkingDir(workingDir)
            .withCmd("bash", "-c", "source .env && $runCmd")
            .withName(containerName)
            .withHostConfig(HostConfig.newHostConfig()
                .withRuntime(settings.runtime)
                // processes from inside the container will be able to access host's network using this hostname
                .withExtraHosts("host.docker.internal:${getHostIp()}")
                .withLogConfig(
                    when (settings.loggingDriver) {
                        "loki" -> LogConfig(
                            LogConfig.LoggingType.LOKI,
                            mapOf(
                                // similar to config in docker-compose.yaml
                                "mode" to "non-blocking",
                                "loki-url" to "http://127.0.0.1:9110/loki/api/v1/push",
                                "loki-external-labels" to "container_name={{.Name}},source=save-agent"
                            )
                        )
                        else -> LogConfig(LogConfig.LoggingType.DEFAULT)
                    }
                )
            )
            .execTimed(meterRegistry, "$DOCKER_METRIC_PREFIX.container.create")

        val containerId = createContainerCmdResponse!!.id
        val envFile = createTempDirectory("orchestrator").resolve(".env").apply {
            writeText("""
                AGENT_ID=$containerId""".trimIndent()
            )
        }
        copyResourcesIntoContainer(
            containerId,
            workingDir,
            listOf(envFile.toFile())
        )

        return containerId
    }

    /**
     * Copies specified [resources] into the container with id [containerId]
     *
     * @param resources additional resources
     * @param containerId id of the target container
     * @param remotePath path in the target container
     */
    internal fun copyResourcesIntoContainer(containerId: String,
                                            remotePath: String,
                                            resources: Collection<File>) {
        createTgzStream(*resources.toTypedArray()).use { out ->
            dockerClient.copyArchiveToContainerCmd(containerId)
                .withTarInputStream(out.toByteArray().inputStream())
                .withRemotePath(remotePath)
                .execTimed(meterRegistry, "$DOCKER_METRIC_PREFIX.container.copy.archive")
        }
    }

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

    /**
     * Add [files] to .tar.gz archive and return the underlying [ByteArrayOutputStream]
     *
     * @param files files to be added to archive
     * @return resulting [ByteArrayOutputStream]
     */
    private fun createTgzStream(vararg files: File): ByteArrayOutputStream {
        val out = ByteArrayOutputStream()
        BufferedOutputStream(out).use { buffOut ->
            GZIPOutputStream(buffOut).use { gzOut ->
                TarArchiveOutputStream(gzOut).use { tgzOut ->
                    files.forEach {
                        tgzOut.putArchiveEntry(TarArchiveEntry(it, it.name))
                        Files.copy(it.toPath(), tgzOut)
                        tgzOut.closeArchiveEntry()
                    }
                    tgzOut.finish()
                }
                gzOut.finish()
            }
            buffOut.flush()
        }
        return out
    }

    companion object {
        private val log = LoggerFactory.getLogger(DockerContainerManager::class.java)
    }
}
