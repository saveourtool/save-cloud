package org.cqfn.save.orchestrator.service

import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.Project
import org.cqfn.save.orchestrator.config.Beans
import org.cqfn.save.orchestrator.config.ConfigProperties
import org.cqfn.save.orchestrator.controller.AgentsController
import org.cqfn.save.orchestrator.testutils.TestConfiguration
import org.cqfn.save.testutils.checkQueues
import org.cqfn.save.testutils.cleanup
import org.cqfn.save.testutils.createMockWebServer
import org.cqfn.save.testutils.enqueue

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension

import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString

@ExtendWith(SpringExtension::class)
@EnableConfigurationProperties(ConfigProperties::class)
@TestPropertySource("classpath:application.properties")
@DisabledOnOs(OS.WINDOWS, disabledReason = "If required, can be run with `docker-tcp` profile and with TCP port enabled on Docker Daemon")
@WebFluxTest(controllers = [AgentsController::class])  // to autowire everything for DockerService
@MockBeans(
    MockBean(AgentService::class),
)
@Import(Beans::class, DockerService::class, TestConfiguration::class)
class DockerServiceTest {
    @Autowired private lateinit var dockerService: DockerService
    private lateinit var testImageId: String
    private lateinit var testContainerId: String

    @Test
    @Suppress("UnsafeCallOnNullableType")
    fun `should create a container with save agent and test resources and start it`() {
        // build base image
        val project = Project.stub(null)
        val testExecution = Execution.stub(project).apply {
            resourcesRootPath = "foo"
            id = 42L
        }
        testContainerId = dockerService.buildAndCreateContainers(testExecution, null).single()
        logger.debug("Created container $testContainerId")

        // start container and query backend
        mockServer.enqueue(
            "/updateExecutionByDto",
            MockResponse()
                .setResponseCode(200)
        )
        dockerService.startContainersAndUpdateExecution(testExecution, listOf(testContainerId))

        // assertions
        Thread.sleep(2_500)  // waiting for container to start
        val inspectContainerResponse = dockerService.dockerContainerManager.dockerClient.inspectContainerCmd(testContainerId).exec()
        testImageId = inspectContainerResponse.imageId
        Assertions.assertTrue(inspectContainerResponse.state.running!!) {
            dockerService.dockerContainerManager.dockerClient.logContainerCmd(testContainerId)
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
    }

    @AfterEach
    fun tearDown() {
        if (::testContainerId.isInitialized) {
            dockerService.dockerContainerManager.dockerClient.removeContainerCmd(testContainerId).exec()
        }
        if (::testImageId.isInitialized) {
            dockerService.dockerContainerManager.dockerClient.removeImageCmd(testImageId).exec()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DockerServiceTest::class.java)

        @JvmStatic
        private val mockServer = createMockWebServer()

        @AfterEach
        fun cleanup() {
            mockServer.checkQueues()
            mockServer.cleanup()
        }

        @AfterAll
        fun teardown() {
            mockServer.shutdown()
        }

        @OptIn(ExperimentalPathApi::class)
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("orchestrator.testResources.basePath") {
                val tmpDir = createTempDirectory("repository")
                Path(tmpDir.pathString, "foo").createDirectory()
                tmpDir.pathString
            }
            registry.add("orchestrator.backendUrl") {
                mockServer.start()
                "http://localhost:${mockServer.port}"
            }
        }
    }
}
