package com.saveourtool.save.orchestrator.service

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.*
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.Project
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.SAVE_AGENT_VERSION
import com.saveourtool.save.orchestrator.config.Beans
import com.saveourtool.save.orchestrator.createTgzStream
import com.saveourtool.save.orchestrator.docker.DockerContainerRunner

import com.saveourtool.save.orchestrator.utils.silentlyCleanupContainer
import com.saveourtool.save.orchestrator.utils.silentlyExec
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.ResponseEntity
import org.springframework.test.context.TestPropertySource
import reactor.kotlin.core.publisher.toMono
import java.net.ServerSocket
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.math.absoluteValue
import kotlin.random.Random

@SpringBootTest
@DisabledOnOs(OS.WINDOWS, disabledReason = "Please run ContainerServiceTestOnWindows")
@Import(
    Beans::class,
    DockerContainerRunner::class,
    ContainerService::class,
    AgentService::class,
)
class ContainerServiceTest {
    @Autowired private lateinit var dockerClient: DockerClient
    @Autowired private lateinit var containerService: ContainerService
    @MockBean private lateinit var orchestratorAgentService: OrchestratorAgentService
    private lateinit var testContainerId: String
    private lateinit var mockserverContainerId: String
    private val mockserverPort: Int = ServerSocket(0).localPort
    private val mockserverImageName = "mockserver/mockserver"
    private val mockserverImageVersion = "5.15.0"
    private val mockserverImageFullName = "$mockserverImageName:$mockserverImageVersion"
    private val mockserverVolumeName = "mockserver-config"
    private val mockserverConfigPath = "/config"
    private val mockUrl = "/some-path-do-download-save-agent"

    @BeforeEach
    fun setUp(@TempDir tmpDir: Path) {
        whenever(orchestratorAgentService.updateExecutionStatus(any(), any(), anyOrNull()))
            .thenReturn(ResponseEntity.ok().build<Void>().toMono())

        val configFile = (tmpDir / "initializerJson.json").createFile()
            .also {
                it.writeText(
                    """
                        [
                          {
                            "httpRequest": {
                              "path": "$mockUrl"
                            },
                            "httpResponse": {
                              "body": "!#/bin/bash\n echo \"sleep\"\n sleep 5000"
                            }
                          }
                       ]
                    """.trimIndent()
                )
            }
        dockerClient.pullImageCmd(mockserverImageName)
            .withTag(mockserverImageVersion)
            .exec(PullImageResultCallback())
            .awaitCompletion()
        dockerClient.createVolumeCmd()
            .withName(mockserverVolumeName)
            .exec()
        mockserverContainerId = dockerClient.createContainerCmd(mockserverImageFullName)
            .withHostName("mockserver")
            .withHostConfig(
                HostConfig.newHostConfig()
                    .withBinds(Bind(mockserverVolumeName, Volume(mockserverConfigPath)))
                    .withPortBindings(
                        Ports(
                            ExposedPort.tcp(1080),
                            Ports.Binding.bindPort(mockserverPort),
                        )
                    )
            )
            .withExposedPorts(ExposedPort.tcp(mockserverPort))
            .withEnv("MOCKSERVER_INITIALIZATION_JSON_PATH=$mockserverConfigPath/${configFile.fileName}")
            .exec()
            .id
            .also { containerId ->
                createTgzStream(configFile.toFile()).use { out ->
                    dockerClient.copyArchiveToContainerCmd(containerId)
                        .withTarInputStream(out.toByteArray().inputStream())
                        .withRemotePath(mockserverConfigPath)
                        .exec()
                }
                dockerClient.startContainerCmd(containerId).exec()
            }

    }

    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION", "HttpUrlsUsage")
    fun `should create a container with save agent and test resources and start it`() {
        val executionId = Random.nextLong().absoluteValue
        // build base image
        val project = Project.stub(null)
        val testExecution = Execution.stub(project).apply {
            id = executionId
            sdk = "Java:11"
            status = ExecutionStatus.PENDING
        }
        val configuration = containerService.prepareConfiguration(
            testExecution.toRunRequest(
                saveAgentUrl = URL("http://host.docker.internal:$mockserverPort$mockUrl"),
            )
        )
        // start container and query backend
        containerService.createAndStartContainers(
            testExecution.requiredId(),
            configuration
        )
        testContainerId = dockerClient.listContainersCmd()
            .withNameFilter(listOf("-${testExecution.requiredId()}-"))
            .exec()
            .map { it.id }
            .single()
        logger.debug("Created container $testContainerId")

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

        verifyNoMoreInteractions(orchestratorAgentService)
    }

    @AfterEach
    fun tearDown() {
        if (::testContainerId.isInitialized) {
            dockerClient.silentlyCleanupContainer(testContainerId)
        }
        if (::mockserverContainerId.isInitialized) {
            dockerClient.silentlyCleanupContainer(mockserverContainerId)
        }
        dockerClient.removeVolumeCmd(mockserverVolumeName).silentlyExec()
        dockerClient.removeImageCmd(mockserverImageFullName).silentlyExec()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ContainerServiceTest::class.java)
    }
}

@EnabledOnOs(OS.WINDOWS)
@TestPropertySource("classpath:META-INF/save-orchestrator-common/application-docker-tcp.properties")
class ContainerServiceTestOnWindows : ContainerServiceTest() {
    init {
        System.setProperty("OVERRIDE_HOST_IP", "host-gateway")
    }
}
