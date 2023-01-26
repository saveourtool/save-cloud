package com.saveourtool.save.orchestrator.docker

import com.saveourtool.save.orchestrator.config.Beans
import com.saveourtool.save.orchestrator.service.ContainerService

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.Image
import com.saveourtool.save.orchestrator.service.OrchestratorAgentService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

import kotlin.io.path.createTempFile

@SpringBootTest
@Import(Beans::class, DockerContainerRunner::class)
@DisabledOnOs(OS.WINDOWS, disabledReason = "Please run DockerContainerManagerTestOnWindows")
class DockerContainerManagerTest {
    @Autowired private lateinit var dockerClient: DockerClient
    @Autowired private lateinit var dockerAgentRunner: DockerContainerRunner
    private lateinit var baseImage: Image
    private lateinit var testContainerId: String
    private lateinit var testImageId: String
    @MockBean private lateinit var orchestratorAgentService: OrchestratorAgentService

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
        dockerAgentRunner.createAndStart(
            executionId = 43,
            configuration = ContainerService.RunConfiguration(
                baseImage.repoTags.first(),
                listOf("bash", "-c", "./script.sh"),
                workingDir = "/",
                env = emptyMap(),
            ),
            replicas = 1,
        )
        testContainerId = dockerClient.listContainersCmd()
            .withNameFilter(listOf("-43-"))
            .exec()
            .map { it.id }
            .single()
        val inspectContainerResponse = dockerClient
            .inspectContainerCmd(testContainerId)
            .exec()

        Assertions.assertEquals("bash", inspectContainerResponse.path)
        Assertions.assertArrayEquals(
            arrayOf("-c", "env \$(cat /home/save-agent/.env | xargs) sh -c \"./script.sh\""),
            inspectContainerResponse.args
        )
        // leading extra slash: https://github.com/moby/moby/issues/6705
        Assertions.assertTrue(inspectContainerResponse.name.startsWith("/save-execution-43"))

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

@EnabledOnOs(OS.WINDOWS)
@TestPropertySource("classpath:META-INF/save-orchestrator-common/application-docker-tcp.properties")
class DockerContainerManagerTestOnWindows : DockerContainerManagerTest() {
    init {
        System.setProperty("OVERRIDE_HOST_IP", "host-gateway")
    }
}
