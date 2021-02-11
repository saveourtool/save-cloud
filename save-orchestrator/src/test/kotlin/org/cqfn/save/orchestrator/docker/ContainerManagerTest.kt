package org.cqfn.save.backend.docker

import org.cqfn.save.domain.RunConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfSystemProperty
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile

@OptIn(ExperimentalPathApi::class)
class ContainerManagerTest {
    private lateinit var containerManager: ContainerManager
    private lateinit var testContainerId: String

    @BeforeEach
    fun setUp() {
        containerManager = if (System.getProperty("os.name").startsWith("Windows")) {
            // for docker inside WSL2 use it's eth0 network IP
            ContainerManager("tcp://172.20.51.70:2375")
        } else {
            // for Linux "it just works"(c) with unix socket
            ContainerManager()
        }
    }

    @Test
    fun `should create a container and copy files into it`() {
        val testFile = createTempFile().toFile()
        testFile.writeText("wow such testing")
        val resourceFile = createTempFile().toFile()
        resourceFile.writeText("Lorem ipsum dolor sit amet")
        testContainerId = containerManager.createWithFile(
            RunConfiguration("./script.sh", testFile.name),
            "testContainer",
            testFile,
            listOf(resourceFile)
        )
        val inspectContainerResponse = containerManager.dockerClient
            .inspectContainerCmd(testContainerId)
            .exec()
        Assertions.assertEquals("./script.sh", inspectContainerResponse.path)
        Assertions.assertEquals(0, inspectContainerResponse.args.size)
        Assertions.assertEquals("/testContainer", inspectContainerResponse.name)
    }

    @Test
    @DisabledIfSystemProperty(named = "os.name", matches = "Windows.*", disabledReason = "Cannot properly use Dockerfiles on Windows")
    fun `should build an image with provided resources`() {
        val resourcesDir = createTempDirectory()
        repeat(5) { createTempFile(resourcesDir) }
        val imageId =  containerManager.buildImageWithResources(baseDir = resourcesDir.toFile(), resourcesPath = "/app/resources")
        val inspectImageResponse = containerManager.dockerClient.inspectImageCmd(imageId).exec()
        Assertions.assertTrue(inspectImageResponse.size!! > 0)
    }

    @AfterEach
    fun tearDown() {
        if (::testContainerId.isInitialized) {
            containerManager.dockerClient.removeContainerCmd(testContainerId).exec()
        }
    }
}
