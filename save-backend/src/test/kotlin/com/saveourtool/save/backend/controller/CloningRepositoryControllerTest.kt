package com.saveourtool.save.backend.controller

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.configs.WebConfig
import com.saveourtool.save.backend.configs.WebSecurityConfig
import com.saveourtool.save.backend.controllers.CloneRepositoryController
import com.saveourtool.save.backend.repository.*
import com.saveourtool.save.backend.security.ProjectPermissionEvaluator
import com.saveourtool.save.backend.service.ExecutionService
import com.saveourtool.save.backend.service.ProjectService
import com.saveourtool.save.backend.service.UserDetailsService
import com.saveourtool.save.backend.storage.AvatarStorage
import com.saveourtool.save.backend.storage.FileStorage
import com.saveourtool.save.backend.utils.ConvertingAuthenticationManager
import com.saveourtool.save.domain.*
import com.saveourtool.save.entities.*
import com.saveourtool.save.testutils.checkQueues
import com.saveourtool.save.testutils.cleanup
import com.saveourtool.save.testutils.createMockWebServer
import com.saveourtool.save.testutils.enqueue
import com.saveourtool.save.utils.toDataBufferFlux
import com.saveourtool.save.v1

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
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
import reactor.core.scheduler.Schedulers

import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.createFile

@WebFluxTest(controllers = [CloneRepositoryController::class])
@Import(
    WebSecurityConfig::class,
    WebConfig::class,
    FileStorage::class,
    AvatarStorage::class,
    ConvertingAuthenticationManager::class,
    UserDetailsService::class,
)
@EnableConfigurationProperties(ConfigProperties::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@MockBeans(
    MockBean(ProjectPermissionEvaluator::class),
    MockBean(UserRepository::class),
)
@Suppress("TOO_LONG_FUNCTION")
class CloningRepositoryControllerTest {
    private val organization = Organization("Huawei", OrganizationStatus.CREATED, 1, null).apply { id = 1 }
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
    @Autowired private lateinit var fileStorage: FileStorage

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockBean
    lateinit var projectService: ProjectService
    @MockBean
    lateinit var executionService: ExecutionService
    @TempDir internal lateinit var tmpDir: Path

    @BeforeEach
    fun webClientSetUp() {
        webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(2)).build()

        whenever(projectService.findByNameAndOrganizationName("huaweiName", "Huawei"))
            .thenReturn(testProject)

        whenever(projectService.findWithPermissionByNameAndOrganization(any(), eq(testProject.name), any(), any(), anyOrNull(), any()))
            .thenAnswer { Mono.just(testProject) }

        whenever(executionService.saveExecution(any()))
            .thenAnswer { it.arguments[0] as Execution }
    }

    @Test
    @WithMockUser(username = "John Doe")
    fun checkNewJobResponse() {
        mockServerPreprocessor.enqueue(
            "/upload",
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
            .uri("/api/$v1/submitExecutionRequest")
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
        fileStorage.upload(ProjectCoordinates("Huawei", "huaweiName"),
            binFile.toFileInfo().toFileKey(),
            binFile.toDataBufferFlux().map { it.asByteBuffer() }).subscribeOn(Schedulers.immediate()).block()
        fileStorage.upload(ProjectCoordinates("Huawei", "huaweiName"),
            property.toFileInfo().toFileKey(),
            property.toDataBufferFlux().map { it.asByteBuffer() }).subscribeOn(Schedulers.immediate()).block()

        val sdk = Jdk("8")
        val request = ExecutionRequestForStandardSuites(testProject, emptyList(), sdk, null, null, null, "version")
        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("execution", request)
        bodyBuilder.part("file", property.toFileInfo())
        bodyBuilder.part("file", binFile.toFileInfo())

        mockServerPreprocessor.enqueue(
            "/uploadBin",
            MockResponse()
                .setResponseCode(202)
                .setBody("Clone pending")
                .addHeader("Content-Type", "application/json")
        )

        webTestClient.post()
            .uri("/api/$v1/executionRequestStandardTests")
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

        @AfterEach
        fun cleanup() {
            mockServerPreprocessor.checkQueues()
            mockServerPreprocessor.cleanup()
        }

        @AfterAll
        fun tearDown() {
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
