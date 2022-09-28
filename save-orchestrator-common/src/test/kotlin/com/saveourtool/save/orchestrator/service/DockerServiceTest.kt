package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.Project
import com.saveourtool.save.orchestrator.config.Beans
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.docker.DockerAgentRunner
import com.saveourtool.save.testutils.checkQueues
import com.saveourtool.save.testutils.cleanup
import com.saveourtool.save.testutils.createMockWebServer
import com.saveourtool.save.testutils.enqueue

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import com.saveourtool.save.execution.ExecutionStatus
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.mockito.kotlin.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.ResponseEntity
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.util.FileSystemUtils
import reactor.kotlin.core.publisher.toMono

import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Paths

@SpringBootTest
@DisabledOnOs(OS.WINDOWS, disabledReason = "Please run DockerServiceTestOnWindows")
@Import(
    Beans::class,
    DockerAgentRunner::class,
    DockerService::class,
    AgentService::class,
)
class DockerServiceTest {
    @Autowired private lateinit var dockerClient: DockerClient
    @Autowired private lateinit var dockerService: DockerService
    @Autowired private lateinit var configProperties: ConfigProperties
    private lateinit var testContainerId: String
    @MockBean private lateinit var agentRepository: AgentRepository

    @BeforeEach
    fun setUp() {
        Files.createDirectories(
            Paths.get(configProperties.testResources.tmpPath)
        )
        whenever(agentRepository.updateExecutionByDto(any(), any(), anyOrNull()))
            .thenReturn(ResponseEntity.ok().build<Void>().toMono())
    }

    @Test
    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
    fun `should create a container with save agent and test resources and start it`() {
        // build base image
        val project = Project.stub(null)
        val testExecution = Execution.stub(project).apply {
            id = 42L
            testSuiteIds = "1,2,3"
            sdk = "Java:11"
            status = ExecutionStatus.PENDING
        }
        val configuration = dockerService.prepareConfiguration(testExecution.toRunRequest())
        testContainerId = dockerService.createContainers(
            testExecution.id!!,
            configuration
        ).single()
        logger.debug("Created container $testContainerId")

        // start container and query backend
        mockServer.enqueue(
            "/internal/files/download-save-agent",
            MockResponse()
                .setHeader("Content-Type", "application/octet-stream")
                .setResponseCode(200)
                .setBody("sleep 200")
        )
        dockerService.startContainersAndUpdateExecution(testExecution.requiredId(), listOf(testContainerId))
            .subscribe()

        // assertions
        Thread.sleep(2_500)  // waiting for container to start
        val inspectContainerResponse = dockerClient.inspectContainerCmd(testContainerId).exec()
        Assertions.assertTrue(inspectContainerResponse.state.running!!) {
            dockerClient.logContainerCmd(testContainerId)
                .withStdOut(true)
                .withStdErr(true)
                .exec(object : ResultCallback.Adapter<Frame>() {
                    override fun onNext(frame: Frame?) {
                        logger.info(frame.toString())
                    }
                })
                .awaitCompletion()
            "container $testContainerId is not running, actual state ${inspectContainerResponse.state}"
        }

        // tear down
        dockerService.stopAgents(listOf(testContainerId))
        verify(agentRepository).updateExecutionByDto(any(), any(), anyOrNull())
        verifyNoMoreInteractions(agentRepository)
    }

    @AfterEach
    fun tearDown() {
        FileSystemUtils.deleteRecursively(
            Paths.get(configProperties.testResources.tmpPath)
        )
        if (::testContainerId.isInitialized) {
            dockerClient.removeContainerCmd(testContainerId).exec()
        }
        mockServer.checkQueues()
        mockServer.cleanup()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DockerServiceTest::class.java)

        @JvmStatic
        private val mockServer = createMockWebServer()

        @JvmStatic
        @AfterAll
        fun teardown() {
            mockServer.shutdown()
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            mockServer.start(
                InetSocketAddress(0).address,
                0
            )
            registry.add("orchestrator.agentSettings.backendUrl") {
                "http://host.docker.internal:${mockServer.port}"
            }
        }
    }
}

@EnabledOnOs(OS.WINDOWS)
@TestPropertySource("classpath:META-INF/save-orchestrator-common/application-docker-tcp.properties")
class DockerServiceTestOnWindows : DockerServiceTest() {
    init {
        System.setProperty("OVERRIDE_HOST_IP", "host-gateway")
    }
}
