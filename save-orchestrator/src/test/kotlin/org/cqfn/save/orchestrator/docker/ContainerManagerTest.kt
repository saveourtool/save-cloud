package org.cqfn.save.orchestrator.docker

import org.cqfn.save.orchestrator.config.ConfigProperties

import com.github.dockerjava.api.command.PullImageResultCallback
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension

import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile

@OptIn(ExperimentalPathApi::class)
@ExtendWith(SpringExtension::class)
@EnableConfigurationProperties(ConfigProperties::class)
@TestPropertySource("classpath:application.properties")
@DisabledOnOs(OS.WINDOWS, disabledReason = "`getHostIp` method uses `getent`, which is unavailable on Windows")
class ContainerManagerTest {
    @Autowired private lateinit var configProperties: ConfigProperties
    private lateinit var containerManager: ContainerManager
    private lateinit var baseImageId: String
    private lateinit var testContainerId: String
    private lateinit var testImageId: String

    @BeforeEach
    fun setUp() {
        containerManager = ContainerManager(configProperties.docker, CompositeMeterRegistry())
        containerManager.dockerClient.pullImageCmd("ubuntu")
            .withTag("latest")
            .exec(PullImageResultCallback())
            .awaitCompletion()
        baseImageId = containerManager.dockerClient.listImagesCmd().exec().first {
            it.repoTags!!.contains("ubuntu:latest")
        }
            .id
    }

    @Test
    fun `should create a container with specified cmd and then copy resources into it`() {
        val testFile = createTempFile().toFile()
        testFile.writeText("wow such testing")
        testContainerId = containerManager.createContainerFromImage(
            baseImageId,
            "/",
            "./script.sh",
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
    @Suppress("UnsafeCallOnNullableType")
    fun `should build an image with provided resources`() {
        val resourcesDir = createTempDirectory()
        repeat(5) { createTempFile(resourcesDir) }
        testImageId = containerManager.buildImageWithResources(
            imageName = "test:test", baseDir = resourcesDir.toFile(), resourcesPath = "/app/resources"
        )
        val inspectImageResponse = containerManager.dockerClient.inspectImageCmd(testImageId).exec()
        Assertions.assertTrue(inspectImageResponse.size!! > 0)
    }

    @AfterEach
    fun tearDown() {
        if (::testContainerId.isInitialized) {
            containerManager.dockerClient.removeContainerCmd(testContainerId).exec()
        }
        if (::testImageId.isInitialized) {
            containerManager.dockerClient.removeImageCmd(testImageId).exec()
        }
    }
}
