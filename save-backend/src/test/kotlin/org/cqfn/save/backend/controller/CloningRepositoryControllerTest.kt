package org.cqfn.save.backend.controller

import org.cqfn.save.backend.controllers.CloneRepositoryController
import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.backend.repository.AgentStatusRepository
import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.repository.GitRepository
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.backend.repository.TestSuiteRepository
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.ExecutionRequestForStandardSuites
import org.cqfn.save.entities.GitDto
import org.cqfn.save.entities.Project

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

import java.io.File
import java.time.Duration

@WebFluxTest(controllers = [CloneRepositoryController::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@MockBeans(
    MockBean(AgentStatusRepository::class),
    MockBean(AgentRepository::class),
    MockBean(ExecutionRepository::class),
    MockBean(ProjectRepository::class),
    MockBean(TestExecutionRepository::class),
    MockBean(TestRepository::class),
    MockBean(TestSuiteRepository::class),
    MockBean(ProjectService::class),
    MockBean(GitRepository::class),
)
class CloningRepositoryControllerTest {
    @Autowired
    lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun webClientSetUp() {
        webTestClient.mutate().responseTimeout(Duration.ofSeconds(2)).build()
    }

    @Test
    fun checkNewJobResponse() {
        mockServerPreprocessor.enqueue(
            MockResponse()
                .setResponseCode(202)
                .setBody("Clone pending")
                .addHeader("Content-Type", "application/json")
        )
        val project = Project("noname", "1", "1", "1")
        val gitRepo = GitDto("1", project = project)
        val executionRequest = ExecutionRequest(project, gitRepo)
        webTestClient.post()
            .uri("/submitExecutionRequest")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(executionRequest))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.ACCEPTED)
            .expectBody<String>()
            .isEqualTo("Clone pending")
    }

    @Test
    fun checkNewJobResponseForBin() {
        val binFile = File("binFilePath")
        val property = File("propertyPath")
        val project = Project("noname", "1", "1", "1")
        val request = ExecutionRequestForStandardSuites(project, emptyList())
        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("execution", request)
        bodyBuilder.part("property", property)
        bodyBuilder.part("binFile", binFile)

        mockServerPreprocessor.enqueue(
            MockResponse()
                .setResponseCode(202)
                .setBody("Clone pending")
                .addHeader("Content-Type", "application/json")
        )

        webTestClient.post()
            .uri("/submitExecutionRequestBin")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.ACCEPTED)
            .expectBody<String>()
            .isEqualTo("Clone pending")
    }

    companion object {
        @JvmStatic lateinit var mockServerPreprocessor: MockWebServer

        @AfterAll
        fun tearDown() {
            mockServerPreprocessor.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            mockServerPreprocessor = MockWebServer()
            mockServerPreprocessor.start()
            registry.add("backend.preprocessorUrl") { "http://localhost:${mockServerPreprocessor.port}" }
        }
    }
}
