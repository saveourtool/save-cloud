package com.saveourtool.save.orchestrator.docker

import com.saveourtool.save.orchestrator.config.Beans
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.service.DockerService
import com.saveourtool.save.orchestrator.testutils.TestConfiguration

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.Image
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
import java.nio.file.Path

import kotlin.io.path.createTempFile

@ExtendWith(SpringExtension::class)
@EnableConfigurationProperties(ConfigProperties::class)
@TestPropertySource("classpath:application.properties")
@Import(Beans::class, DockerAgentRunner::class, TestConfiguration::class)
@DisabledOnOs(OS.WINDOWS, disabledReason = "If required, can be run with `docker-tcp` profile and corresponding .properties file and with TCP port enabled on Docker Daemon")
class DockerContainerManagerTest {
    @Autowired private lateinit var dockerClient: DockerClient
    @Autowired private lateinit var dockerAgentRunner: DockerAgentRunner
    private lateinit var baseImage: Image
    private lateinit var testContainerId: String
    private lateinit var testImageId: String

    @BeforeEach
    fun setUp() {
        dockerClient.pullImageCmd("ghcr.io/saveourtool/save-base")
            .withRegistry("https://ghcr.io")
            .withTag("eclipse-temurin-11")
            .exec(PullImageResultCallback())
            .awaitCompletion()
        baseImage = dockerClient.listImagesCmd()
            .exec()
            .first {
                it.repoTags?.contains("ghcr.io/saveourtool/save-base:eclipse-temurin-11") == true
            }
        dockerClient.createVolumeCmd().withName("test-volume").exec()
    }

    @Test
    fun `should create a container with specified cmd and then copy resources into it`() {
        val testFile = createTempFile().toFile()
        testFile.writeText("wow such testing")
        testContainerId = dockerAgentRunner.create(
            executionId = 42,
            configuration = DockerService.RunConfiguration(
                baseImage.repoTags.first(),
                listOf("bash", "-c", "./script.sh"),
                DockerPvId("test-volume"),
                workingDir = "/",
                resourcesPath = Path.of("test-resources-path"),
                resourcesConfiguration = DockerService.RunConfiguration.ResourcesConfiguration(
                    executionId = 99L,
                    additionalFilesString = "",
                )
            ),
            replicas = 1,
        ).single()
        val inspectContainerResponse = dockerClient
            .inspectContainerCmd(testContainerId)
            .exec()

        Assertions.assertEquals("bash", inspectContainerResponse.path)
        Assertions.assertArrayEquals(
            arrayOf("-c", "env \$(cat /home/save-agent/.env | xargs) sh -c \"cp /home/save-agent/resources/* . && ./script.sh\""),
            inspectContainerResponse.args
        )
        // leading extra slash: https://github.com/moby/moby/issues/6705
        Assertions.assertTrue(inspectContainerResponse.name.startsWith("/save-execution-42"))

        val resourceFile = createTempFile().toFile()
        resourceFile.writeText("Lorem ipsum dolor sit amet")
        dockerAgentRunner.copyResourcesIntoContainer(testContainerId, "/var", listOf(testFile, resourceFile))
    }

    @AfterEach
    fun tearDown() {
        if (::testContainerId.isInitialized) {
            dockerClient.removeContainerCmd(testContainerId).exec()
        }
        if (::testImageId.isInitialized) {
            dockerClient.removeImageCmd(testImageId).exec()
        }
        dockerClient.removeVolumeCmd("test-volume").exec()
    }
}
