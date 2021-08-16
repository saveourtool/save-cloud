package org.cqfn.save.backend.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.backend.controllers.CloneRepositoryController
import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.backend.repository.AgentStatusRepository
import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.repository.GitRepository
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.backend.repository.TestSuiteRepository
import org.cqfn.save.backend.service.ExecutionService
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.domain.Jdk
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
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.http.codec.multipart.FilePart
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Flux
import java.io.File

import java.time.Duration

@WebFluxTest(controllers = [CloneRepositoryController::class])
@EnableConfigurationProperties(ConfigProperties::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@MockBeans(
    MockBean(AgentStatusRepository::class),
    MockBean(AgentRepository::class),
    MockBean(ExecutionRepository::class),
    MockBean(ExecutionService::class),
    MockBean(TestExecutionRepository::class),
    MockBean(TestRepository::class),
    MockBean(TestSuiteRepository::class),
    MockBean(ProjectRepository::class),
    MockBean(GitRepository::class),
)
class CloningRepositoryControllerTest {
    @Autowired private lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockBean
    lateinit var projectService: ProjectService

    @TempDir internal lateinit var tmpDir: File

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
        val project = Project("Huawei", "huaweiName", "huawei.com", "test description")
        Mockito
            .`when`(projectService.getProjectByNameAndOwner("huaweiName", "Huawei"))
            .thenReturn(project)
        val sdk = Jdk("8")
        val gitRepo = GitDto("1")
        val executionRequest = ExecutionRequest(project, gitRepo, sdk = sdk, executionId = null)
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
        val binFile: File = tmpDir.resolve("binFile").apply {
            createNewFile()
        }
        val property: File = tmpDir.resolve("property").apply {
            createNewFile()
        }

        val project = Project("Huawei", "huaweiName", "huawei.com", "test description").apply {
            id = 1
        }
        val sdk = Jdk("8")
        val request = ExecutionRequestForStandardSuites(project, emptyList(), sdk)
        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("execution", request)
        bodyBuilder.part("file", FileSystemResource(property))
        bodyBuilder.part("file", FileSystemResource(binFile))

        mockServerPreprocessor.enqueue(
            MockResponse()
                .setResponseCode(202)
                .setBody("Clone pending")
                .addHeader("Content-Type", "application/json")
        )
        Mockito
            .`when`(projectService.getProjectByNameAndOwner("huaweiName", "Huawei"))
            .thenReturn(project)

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
