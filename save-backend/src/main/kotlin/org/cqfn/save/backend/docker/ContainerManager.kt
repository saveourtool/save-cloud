package org.cqfn.save.backend.docker

import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.cqfn.save.domain.RunConfiguration
import org.slf4j.LoggerFactory

import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.GZIPOutputStream

class ContainerManager(private val dockerHost: String = "unix:///var/run/docker.sock") {
    private val dockerClientConfig: DockerClientConfig = DefaultDockerClientConfig
        .createDefaultConfigBuilder()
        .withDockerHost(dockerHost)
        .withDockerTlsVerify(false)
        .build()
    private val dockerHttpClient: DockerHttpClient = ApacheDockerHttpClient.Builder()
        .dockerHost(dockerClientConfig.dockerHost)
        .build()
    internal val dockerClient = DockerClientImpl.getInstance(dockerClientConfig, dockerHttpClient)

    /**
     * Creates a docker container with [file], prepared to execute it
     *
     * @param runConfiguration a [RunConfiguration] for the supplied binary
     * @param file an executable file
     * @param resources additional files to be copied in the container too
     * @return id of created container or null if it wasn't created
     */
    fun createWithFile(runConfiguration: RunConfiguration,
                       file: File,
                       resources: Collection<File> = emptySet()): String? {
        // ensure the image is present in the system
        dockerClient.pullImageCmd("docker.io/library/ubuntu")
            .withTag("latest")
            .start()
            .awaitCompletion()

        val createContainerCmdResponse = dockerClient.createContainerCmd("ubuntu:latest")
            .withCmd(runConfiguration.startCommand)
            .withName("testContainer")
            .withHostConfig(HostConfig.newHostConfig()
                .withRuntime("runsc")
            )
            .exec()
        if (createContainerCmdResponse.id == null) {  // todo check if this condition is correct
            log.error("Error creating container: response from daemon is $createContainerCmdResponse")
            return null
        }

        createTgzStream(file, *resources.toTypedArray()).use { out ->
            dockerClient.copyArchiveToContainerCmd(createContainerCmdResponse.id)
                .withTarInputStream(out.toByteArray().inputStream())
                .withRemotePath("/run")
                .exec()
        }
        return createContainerCmdResponse.id
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
                        tgzOut.putArchiveEntry(TarArchiveEntry(it))
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
    }
}
