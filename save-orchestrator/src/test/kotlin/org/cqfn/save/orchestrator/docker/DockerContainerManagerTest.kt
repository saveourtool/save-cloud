package org.cqfn.save.orchestrator.docker

import com.github.dockerjava.api.DockerClient
import org.cqfn.save.orchestrator.config.ConfigProperties

import com.github.dockerjava.api.command.PullImageResultCallback
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import org.cqfn.save.orchestrator.config.Beans
import org.cqfn.save.orchestrator.service.DockerService
import org.cqfn.save.orchestrator.testutils.TestConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension

import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile

@ExtendWith(SpringExtension::class)
@EnableConfigurationProperties(ConfigProperties::class)
@TestPropertySource("classpath:application.properties")
@Import(Beans::class, DockerAgentRunner::class, TestConfiguration::class)
@DisabledOnOs(OS.WINDOWS, disabledReason = "If required, can be run with `docker-tcp` profile and with TCP port enabled on Docker Daemon")
class DockerContainerManagerTest {
    @Autowired private lateinit var dockerClient: DockerClient
    @Autowired private lateinit var dockerAgentRunner: DockerAgentRunner
    private lateinit var dockerContainerManager: DockerContainerManager
    private lateinit var baseImageId: String
    private lateinit var testContainerId: String
    private lateinit var testImageId: String

    @BeforeEach
    fun setUp() {
        dockerContainerManager = DockerContainerManager(CompositeMeterRegistry(), dockerClient)
        dockerClient.pullImageCmd("ubuntu")
            .withTag("latest")
            .exec(PullImageResultCallback())
            .awaitCompletion()
        baseImageId = dockerClient.listImagesCmd().exec().first {
            it.repoTags!!.contains("ubuntu:latest")
        }
            .id
    }

    @Test
    fun `should create a container with specified cmd and then copy resources into it`() {
        val testFile = createTempFile().toFile()
        testFile.writeText("wow such testing")
        testContainerId = dockerAgentRunner.create(
            executionId = 42,
            baseImageId = baseImageId,
            replicas = 1,
            workingDir = "/",
            agentRunCmd = "./script.sh",
        ).single()
        val inspectContainerResponse = dockerClient
            .inspectContainerCmd(testContainerId)
            .exec()

        Assertions.assertEquals("bash", inspectContainerResponse.path)
        Assertions.assertArrayEquals(
            arrayOf("-c", "env \$(cat .env | xargs) ./script.sh"),
            inspectContainerResponse.args
        )
        Assertions.assertEquals("/testContainer", inspectContainerResponse.name)

        val resourceFile = createTempFile().toFile()
        resourceFile.writeText("Lorem ipsum dolor sit amet")
        dockerAgentRunner.copyResourcesIntoContainer(testContainerId, "/var", listOf(testFile, resourceFile))
    }

    @Test
    @Suppress("UnsafeCallOnNullableType")
    fun `should build an image with provided resources`() {
        val resourcesDir = createTempDirectory()
        repeat(5) { createTempFile(resourcesDir) }
        testImageId = dockerContainerManager.buildImageWithResources(
            imageName = "test:test", baseDir = resourcesDir.toFile(), resourcesPath = "/app/resources"
        )
        val inspectImageResponse = dockerClient.inspectImageCmd(testImageId).exec()
        Assertions.assertTrue(inspectImageResponse.size!! > 0)
    }

    @AfterEach
    fun tearDown() {
        if (::testContainerId.isInitialized) {
            dockerClient.removeContainerCmd(testContainerId).exec()
        }
        if (::testImageId.isInitialized) {
            dockerClient.removeImageCmd(testImageId).exec()
        }
    }
}
