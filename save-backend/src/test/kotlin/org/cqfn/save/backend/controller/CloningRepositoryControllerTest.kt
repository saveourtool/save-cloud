package org.cqfn.save.backend.controller

import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.backend.configs.WebConfig
import org.cqfn.save.backend.configs.WebSecurityConfig
import org.cqfn.save.backend.controllers.CloneRepositoryController
import org.cqfn.save.backend.controllers.OrganizationController
import org.cqfn.save.backend.repository.*
import org.cqfn.save.backend.scheduling.StandardSuitesUpdateScheduler
import org.cqfn.save.backend.security.ProjectPermissionEvaluator
import org.cqfn.save.backend.service.ExecutionService
import org.cqfn.save.backend.service.OrganizationService
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.backend.service.UserDetailsService
import org.cqfn.save.backend.utils.ConvertingAuthenticationManager
import org.cqfn.save.domain.Jdk
import org.cqfn.save.domain.toFileInfo
import org.cqfn.save.entities.*
import org.cqfn.save.testutils.checkQueues
import org.cqfn.save.testutils.createMockWebServer
import org.cqfn.save.testutils.enqueue

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Mono

import java.nio.file.Path
import java.time.Duration

import kotlin.io.path.createFile

@WebFluxTest(controllers = [CloneRepositoryController::class])
@Import(
    WebSecurityConfig::class,
    WebConfig::class,
    TimestampBasedFileSystemRepository::class,
    ConvertingAuthenticationManager::class,
    UserDetailsService::class,
)
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
    MockBean(StandardSuitesUpdateScheduler::class),
    MockBean(UserRepository::class),
    MockBean(AwesomeBenchmarksRepository::class),
    MockBean(OrganizationRepository::class),
    MockBean(OrganizationController::class),
    MockBean(OrganizationService::class),
    MockBean(LnkUserProjectRepository::class),
    MockBean(ProjectPermissionEvaluator::class),
)
@Suppress("TOO_LONG_FUNCTION")
class CloningRepositoryControllerTest {
    private val organization = Organization("Huawei", 1, null).apply { id = 1 }
    private var testProject: Project = Project(
        organization = organization,
        name = "huaweiName",
        url = "huawei.com",
        description = "test description",
        status = ProjectStatus.CREATED,
        userId = 1,
    ).apply {
        id = 1
    }
    @Autowired private lateinit var fileSystemRepository: TimestampBasedFileSystemRepository

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockBean
    lateinit var projectService: ProjectService
    @TempDir internal lateinit var tmpDir: Path

    @BeforeEach
    fun webClientSetUp() {
        webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(2)).build()

        whenever(projectService.findByNameAndOrganizationName("huaweiName", "Huawei"))
            .thenReturn(testProject)

        whenever(projectService.findWithPermissionByNameAndOrganization(any(), eq(testProject.name), any(), any(), anyOrNull(), any()))
            .thenAnswer { Mono.just(testProject) }
    }

    @Test
    @WithMockUser(username = "John Doe")
    fun checkNewJobResponse() {
        mockServerPreprocessor.enqueue(
            "/upload$",
            MockResponse()
                .setResponseCode(202)
                .setBody("Clone pending")
                .addHeader("Content-Type", "application/json")
        )
        val sdk = Jdk("8")
        val gitRepo = GitDto("1")
        val executionRequest = ExecutionRequest(testProject, gitRepo, sdk = sdk, executionId = null, testRootPath = ".")
        val multipart = MultipartBodyBuilder().apply {
            part("executionRequest", executionRequest)
        }
            .build()
        webTestClient.post()
            .uri("/api/submitExecutionRequest")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(multipart))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.ACCEPTED)
            .expectBody<String>()
            .isEqualTo("Clone pending")
    }

    @Test
    @WithMockUser(username = "John Doe")
    fun checkNewJobResponseForBin() {
        val binFile = tmpDir.resolve("binFile").apply {
            createFile()
        }
        val property = tmpDir.resolve("property").apply {
            createFile()
        }
        fileSystemRepository.saveFile(binFile)
        fileSystemRepository.saveFile(property)

        val sdk = Jdk("8")
        val request = ExecutionRequestForStandardSuites(testProject, emptyList(), sdk, null, null)
        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("execution", request)
        bodyBuilder.part("file", property.toFileInfo())
        bodyBuilder.part("file", binFile.toFileInfo())

        mockServerPreprocessor.enqueue(
            "/uploadBin$",
            MockResponse()
                .setResponseCode(202)
                .setBody("Clone pending")
                .addHeader("Content-Type", "application/json")
        )

        webTestClient.post()
            .uri("/api/executionRequestStandardTests")
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
            mockServerPreprocessor.checkQueues()
            mockServerPreprocessor.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            mockServerPreprocessor = createMockWebServer()
            mockServerPreprocessor.start()
            registry.add("backend.preprocessorUrl") { "http://localhost:${mockServerPreprocessor.port}" }
        }
    }
}
