package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.SaveApplication
import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.ExecutionRepository
import com.saveourtool.save.backend.repository.ProjectRepository
import com.saveourtool.save.backend.repository.TestExecutionRepository
import com.saveourtool.save.backend.utils.AuthenticationDetails
import com.saveourtool.save.backend.utils.MySqlExtension
import com.saveourtool.save.backend.utils.mutateMockedUser
import com.saveourtool.save.domain.FileKey
import com.saveourtool.save.domain.Jdk
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.entities.RunExecutionRequest
import com.saveourtool.save.testutils.checkQueues
import com.saveourtool.save.testutils.cleanup
import com.saveourtool.save.testutils.createMockWebServer
import com.saveourtool.save.testutils.enqueue
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.v1
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.concurrent.TimeUnit

@ActiveProfiles("test")
@SpringBootTest(classes = [SaveApplication::class])
@EnableConfigurationProperties(ConfigProperties::class)
@AutoConfigureWebTestClient(timeout = "6000000000")
@ExtendWith(MySqlExtension::class)
class RunExecutionControllerTest(
    @Autowired private var webClient: WebTestClient,
) {
    @Autowired private lateinit var projectRepository: ProjectRepository
    @Autowired private lateinit var executionRepository: ExecutionRepository
    @Autowired private lateinit var testExecutionRepository: TestExecutionRepository

    @WithMockUser("admin")
    @Test
    fun trigger() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 1)
        }
        val project = projectRepository.findById(PROJECT_ID).get()
        val request = RunExecutionRequest(
            projectCoordinates = ProjectCoordinates(
                organizationName = project.organization.name,
                projectName = project.name
            ),
            testSuiteIds = listOf(2L, 3L),
            files = listOf(FileKey("test1", 123L)),
            sdk = Jdk("8"),
            execCmd = "execCmd",
            batchSizeForAnalyzer = "batchSizeForAnalyzer",
        )

        // /initializeAgents
        mockServerOrchestrator.enqueue(
            "/initializeAgents",
            MockResponse()
                .setResponseCode(200)
        )
        val assertions = sequence {
            yield(mockServerOrchestrator.takeRequest(60, TimeUnit.SECONDS))
        }.onEach {
            log.info("Request $it")
        }

        val originalExecutionIds = executionRepository.findAll()
            .map { it.requiredId() }
            .toList()
        val originalTestExecutionIds = testExecutionRepository.findAll()
            .map { it.requiredId() }
            .toList()
        webClient.post()
            .uri("/api/$v1/run/trigger")
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isAccepted
        Thread.sleep(15_000) // Time for request to create required entities

        assertions.forEach { Assertions.assertNotNull(it) }
        val newExecutions = executionRepository.findAll().toList()
        Assertions.assertEquals(originalExecutionIds.size + 1, newExecutions.size)
        val newExecution = newExecutions.first { it.requiredId() !in originalExecutionIds }
        Assertions.assertEquals(project, newExecution.project)
        Assertions.assertEquals("admin", newExecution.user?.name)
        Assertions.assertEquals("2,3", newExecution.testSuiteIds)
        Assertions.assertEquals(24, newExecution.allTests)
        Assertions.assertEquals("test1:123", newExecution.additionalFiles)
        Assertions.assertEquals("eclipse-temurin:8", newExecution.sdk)
        Assertions.assertEquals("execCmd", newExecution.execCmd)
        Assertions.assertEquals("batchSizeForAnalyzer", newExecution.batchSizeForAnalyzer)

        val newTestExecutions = testExecutionRepository.findAll().toList()
        Assertions.assertEquals(originalTestExecutionIds.size + 24, newTestExecutions.size)
    }


    @WithMockUser("admin")
    @Test
    fun reTrigger() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 1)
        }

        // /initializeAgents
        mockServerOrchestrator.enqueue(
            "/initializeAgents",
            MockResponse()
                .setResponseCode(200)
        )
        val assertions = sequence {
            yield(mockServerOrchestrator.takeRequest(60, TimeUnit.SECONDS))
        }.onEach {
            log.info("Request $it")
        }

        val originalExecution = executionRepository.findById(EXECUTION_ID).get()
        val originalExecutionIds = executionRepository.findAll()
            .map { it.requiredId() }
            .toList()
        val originalTestExecutionIds = testExecutionRepository.findAll()
            .map { it.requiredId() }
            .toList()
        webClient.post()
            .uri("/api/$v1/run/re-trigger?executionId=${EXECUTION_ID}")
            .exchange()
            .expectStatus()
            .isAccepted
        Thread.sleep(15_000) // Time for request to create required entities

        assertions.forEach { Assertions.assertNotNull(it) }
        val newExecutions = executionRepository.findAll().toList()
        Assertions.assertEquals(originalExecutionIds.size + 1, newExecutions.size)
        val newExecution = newExecutions.first { it.requiredId() !in originalExecutionIds }
        Assertions.assertEquals(originalExecution.project, newExecution.project)
        Assertions.assertEquals("admin", newExecution.user?.name)
        Assertions.assertEquals(originalExecution.testSuiteIds, newExecution.testSuiteIds)
        Assertions.assertEquals(originalExecution.allTests, newExecution.allTests)
        Assertions.assertEquals(originalExecution.additionalFiles, newExecution.additionalFiles)
        Assertions.assertEquals(originalExecution.sdk, newExecution.sdk)
        Assertions.assertEquals(originalExecution.execCmd, newExecution.execCmd)
        Assertions.assertEquals(originalExecution.batchSizeForAnalyzer, newExecution.batchSizeForAnalyzer)

        val newTestExecutions = testExecutionRepository.findAll().toList()
        Assertions.assertEquals(originalTestExecutionIds.size + 1, newTestExecutions.size)
    }

    @TestConfiguration
    class AdditionalConfiguration {
        @Bean fun meterRegistry() = SimpleMeterRegistry()
    }

    companion object {
        private val log: Logger = getLogger<RunExecutionControllerTest>()
        private const val PROJECT_ID = 1L
        private const val EXECUTION_ID = 6L

        @JvmStatic
        lateinit var mockServerOrchestrator: MockWebServer

        @AfterEach
        fun cleanup() {
            mockServerOrchestrator.checkQueues()
            mockServerOrchestrator.cleanup()
        }

        @AfterAll
        fun tearDown() {
            mockServerOrchestrator.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            mockServerOrchestrator = createMockWebServer()
            mockServerOrchestrator.start()
            registry.add("backend.orchestratorUrl") { "http://localhost:${mockServerOrchestrator.port}" }
        }
    }
}