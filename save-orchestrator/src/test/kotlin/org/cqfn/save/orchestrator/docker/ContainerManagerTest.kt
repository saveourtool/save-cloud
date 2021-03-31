package org.cqfn.save.orchestrator.docker

import com.github.dockerjava.api.command.PullImageResultCallback
import org.cqfn.save.domain.RunConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfSystemProperty
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile

@OptIn(ExperimentalPathApi::class)
class ContainerManagerTest {
    private lateinit var testContainerId: String

    @Test
    fun `should create a container with specified cmd and then copy resources into it`() {
        val testFile = createTempFile().toFile()
        testFile.writeText("wow such testing")
        testContainerId = containerManager.createContainerFromImage(
            imageId,
            RunConfiguration("./script.sh", testFile.name),
            "testContainer"
        )
        val inspectContainerResponse = containerManager.dockerClient
            .inspectContainerCmd(testContainerId)
            .exec()

        Assertions.assertEquals("./script.sh", inspectContainerResponse.path)
        Assertions.assertEquals(0, inspectContainerResponse.args.size)
        Assertions.assertEquals("/testContainer", inspectContainerResponse.name)

        val resourceFile = createTempFile().toFile()
        resourceFile.writeText("Lorem ipsum dolor sit amet")
        containerManager.copyResourcesIntoContainer(testContainerId, "/var", listOf(testFile, resourceFile))
    }

    @Test
    @DisabledIfSystemProperty(named = "os.name", matches = "Windows.*", disabledReason = "Cannot properly use Dockerfiles on Windows")
    fun `should build an image with provided resources`() {
        val resourcesDir = createTempDirectory()
        repeat(5) { createTempFile(resourcesDir) }
        val imageId = containerManager.buildImageWithResources(baseDir = resourcesDir.toFile(), resourcesPath = "/app/resources")
        val inspectImageResponse = containerManager.dockerClient.inspectImageCmd(imageId).exec()
        Assertions.assertTrue(inspectImageResponse.size!! > 0)
    }

    @AfterEach
    fun tearDown() {
        if (::testContainerId.isInitialized) {
            containerManager.dockerClient.removeContainerCmd(testContainerId).exec()
        }
    }

    companion object {
        private lateinit var containerManager: ContainerManager
        private lateinit var imageId: String

        @BeforeAll
        @JvmStatic
        fun setUp() {
            containerManager = if (System.getProperty("os.name").startsWith("Windows")) {
                // for docker inside WSL2 use it's eth0 network IP: `ip a | grep eth0`
                // for Docker Desktop, daemon can be exposed on localhost via tcp://localhost:2375, but runsc runtime can't be installed on windows.
                ContainerManager("tcp://172.25.71.25:2375")
            } else {
                // for Linux "it just works"(c) with unix socket
                ContainerManager("unix:///var/run/docker.sock")
            }
            containerManager.dockerClient.pullImageCmd("ubuntu")
                .withTag("latest")
                .exec(PullImageResultCallback())
                .awaitCompletion()
            imageId = containerManager.dockerClient.listImagesCmd().exec().find {
                it.repoTags.firstOrNull() == "ubuntu:latest"
            }!!
                .id
        }
    }
}
