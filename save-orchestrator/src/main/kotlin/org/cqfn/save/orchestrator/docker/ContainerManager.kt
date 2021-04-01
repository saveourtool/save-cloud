package org.cqfn.save.orchestrator.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.BuildImageResultCallback
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.slf4j.LoggerFactory

import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.GZIPOutputStream

import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile

/**
 * A class that communicates with docker daemon
 *
 * @property dockerHost a URL of docker daemon, local unix socket by default
 */
class ContainerManager(private val dockerHost: String) {
    private val dockerClientConfig: DockerClientConfig = DefaultDockerClientConfig
        .createDefaultConfigBuilder()
        .withDockerHost(dockerHost)
        .withDockerTlsVerify(false)
        .build()
    private val dockerHttpClient: DockerHttpClient = ApacheDockerHttpClient.Builder()
        .dockerHost(dockerClientConfig.dockerHost)
        .build()

    /**
     * Main class from docker-java-api
     */
    internal val dockerClient: DockerClient = DockerClientImpl.getInstance(dockerClientConfig, dockerHttpClient)

    /**
     * Creates a docker container
     *
     * @param runCmd an entrypoint for docker container with CLI arguments
     * @param containerName a name for the created container
     * @param baseImageId id of the base docker image for this container
     * @return id of created container or null if it wasn't created
     * @throws DockerException if docker daemon has returned an error
     * @throws RuntimeException if an exception not specific to docker has occurred
     */
    internal fun createContainerFromImage(baseImageId: String,
                                          runCmd: List<String>,
                                          containerName: String): String {
        val baseImage = dockerClient.listImagesCmd().exec().find {
            // fixme: sometimes createImageCmd returns short id without prefix, sometimes full and with prefix.
            it.id.replaceFirst("sha256:", "").startsWith(baseImageId.replaceFirst("sha256:", ""))
        }
            ?: error("Image with requested baseImageId=$baseImageId is not present in the system")
        // createContainerCmd accepts image name, not id, so we retrieve it from tags
        val createContainerCmdResponse = dockerClient.createContainerCmd(baseImage.repoTags.first())
            .withCmd(runCmd)
            .withName(containerName)
            .withHostConfig(HostConfig.newHostConfig()
                .withRuntime("runsc")
                // processes from inside the container will be able to access host's network using this hostname
                .withExtraHosts("host.docker.internal:host-gateway")
            )
            .exec()

        return createContainerCmdResponse.id
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
                .exec()
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
    @OptIn(ExperimentalPathApi::class)
    internal fun buildImageWithResources(baseImage: String = "ubuntu:latest",
                                         imageName: String,
                                         baseDir: File,
                                         resourcesPath: String,
                                         runCmd: String = "RUN /bin/bash",
    ): String {
        val tmpDir = createTempDirectory().toFile()
        baseDir.copyRecursively(File(tmpDir, "resources"))
        val dockerFileAsText =
                """
                    |FROM $baseImage
                    |COPY resources $resourcesPath
                    |$runCmd
                """.trimMargin()
        val dockerFile = createTempFile(tmpDir.toPath()).toFile()
        dockerFile.writeText(dockerFileAsText)
        val buildImageResultCallback: BuildImageResultCallback = try {
            dockerClient.buildImageCmd(dockerFile)
                .withBaseDirectory(tmpDir)
                .withTags(setOf(imageName))
                .start()
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
        private val log = LoggerFactory.getLogger(ContainerManager::class.java)

        // fixme: choose proper base image for tests
        private const val BASE_IMAGE = "ubuntu"
        private const val BASE_IMAGE_TAG = "latest"
        private const val DOCKER_REPO = "docker.io/library/$BASE_IMAGE"
    }
}
