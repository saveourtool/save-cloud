package org.cqfn.save.orchestrator.service

import org.cqfn.save.entities.Execution
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.orchestrator.config.ConfigProperties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfSystemProperty
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString

@ExtendWith(SpringExtension::class)
@EnableConfigurationProperties(ConfigProperties::class)
@TestPropertySource("classpath:application.properties")
@DisabledIfSystemProperty(named = "os.name", matches = "Windows.*", disabledReason = "Docker daemon behaves differently on Windows, and our target platform is Linux")
class DockerServiceTest {
    @Autowired private lateinit var configProperties: ConfigProperties

    private lateinit var dockerService: DockerService
    private lateinit var testContainerId: String

    @BeforeEach
    fun setUp() {
        dockerService = DockerService(configProperties)
    }

    @Test
    fun `should create a container with save agent and test resources and start it`() {
        val testExecution = Execution(0, LocalDateTime.now(), LocalDateTime.now(), ExecutionStatus.PENDING, "1", "foo")
        testContainerId = dockerService.buildAndCreateContainer(testExecution)
        println("Created container $testContainerId")
        dockerService.containerManager.dockerClient.startContainerCmd(testContainerId).exec()
        Thread.sleep(2_500)  // waiting for container to start
        val inspectContainerResponse = dockerService.containerManager.dockerClient.inspectContainerCmd(testContainerId).exec()
        Assertions.assertTrue(inspectContainerResponse.state.running!!) { "container $testContainerId is not running, actual state ${inspectContainerResponse.state}" }
        dockerService.containerManager.dockerClient.stopContainerCmd(testContainerId).exec()
    }

    @AfterEach
    fun tearDown() {
        dockerService.containerManager.dockerClient.removeContainerCmd(testContainerId).exec()
    }

    companion object {
        @OptIn(ExperimentalPathApi::class)
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("orchestrator.testResources.basePath") {
                val tmpDir = createTempDirectory("repository")
                Path(tmpDir.pathString, "foo").createDirectory()
                tmpDir.pathString
            }
        }
    }
}
