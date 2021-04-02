package org.cqfn.save.backend.controller

import org.cqfn.save.backend.controllers.CloneRepositoryController
import org.cqfn.save.backend.repository.AgentStatusRepository
import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.backend.repository.TestSuiteRepository
import org.cqfn.save.backend.service.ExecutionService
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.backend.service.TestExecutionService
import org.cqfn.save.backend.service.TestService
import org.cqfn.save.backend.service.TestSuitesService
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.Project
import org.cqfn.save.repository.GitRepository

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

import java.time.Duration

@WebFluxTest
@Import(CloneRepositoryController::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CloningRepositoryControllerTest {
    @Autowired
    lateinit var webTestClient: WebTestClient
    lateinit var mockServer: MockWebServer
    lateinit var cloneRepositoryController: CloneRepositoryController

    @MockBean
    lateinit var repository: ProjectRepository

    @MockBean
    lateinit var agentStatusRepository: AgentStatusRepository

    @MockBean
    lateinit var projectService: ProjectService

    @MockBean
    lateinit var testExecutionRepository: TestExecutionRepository

    @MockBean
    lateinit var executionRepository: ExecutionRepository

    @MockBean
    lateinit var testRepository: TestRepository

    @MockBean
    lateinit var testSuiteRepository: TestSuiteRepository

    @MockBean
    lateinit var testExecutionService: TestExecutionService

    @MockBean
    lateinit var executionService: ExecutionService

    @MockBean
    lateinit var testService: TestService

    @MockBean
    lateinit var testSuitesService: TestSuitesService

    @BeforeAll
    fun startServer() {
        mockServer = MockWebServer()
        mockServer.start(mockServerPort)
    }

    @AfterAll
    fun stopServer() {
        mockServer.shutdown()
    }

    @BeforeEach
    fun webClientSetUp() {
        webTestClient.mutate().responseTimeout(Duration.ofSeconds(2)).build()
    }

    @Test
    fun checkNewJobResponse() {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(202)
                .setBody("Clone pending")
                .addHeader("Content-Type", "application/json")
        )
        val project = Project("noname", "1", "1", "1", "1")
        val gitRepo = GitRepository("1")
        val executionRequest = ExecutionRequest(project, gitRepo)
        webTestClient.post()
            .uri("/submitExecutionRequest")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(executionRequest))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.ACCEPTED)
            .expectBody(String::class.java).equals("Clone pending")
    }

    companion object {
        private val mockServerPort = 8081
    }
}
