package com.saveourtool.save.backend

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.authservice.config.NoopWebSecurityConfig
import com.saveourtool.save.authservice.utils.AuthenticationDetails
import com.saveourtool.save.backend.configs.WebConfig
import com.saveourtool.save.backend.controllers.DownloadFilesController
import com.saveourtool.save.backend.controllers.FileController
import com.saveourtool.save.backend.controllers.internal.FileInternalController
import com.saveourtool.save.backend.repository.*
import com.saveourtool.save.backend.security.ProjectPermissionEvaluator
import com.saveourtool.save.backend.service.*
import com.saveourtool.save.backend.storage.*
import com.saveourtool.save.backend.utils.mutateMockedUser
import com.saveourtool.save.core.result.DebugInfo
import com.saveourtool.save.core.result.Pass
import com.saveourtool.save.domain.*
import com.saveourtool.save.entities.*
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.utils.toDataBufferFlux
import com.saveourtool.save.v1

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.*

@ActiveProfiles("test")
@WebFluxTest(controllers = [DownloadFilesController::class, FileController::class, FileInternalController::class])
@Import(
    WebConfig::class,
    NoopWebSecurityConfig::class,
    MigrationFileStorage::class,
    FileStorage::class,
    NewFileStorage::class,
    AvatarStorage::class,
    DebugInfoStorage::class,
    ExecutionInfoStorage::class,
    S11nTestConfig::class,
)
@AutoConfigureWebTestClient
@EnableConfigurationProperties(ConfigProperties::class)
@MockBeans(
    MockBean(OrganizationService::class),
    MockBean(UserDetailsService::class),
    MockBean(ExecutionService::class),
    MockBean(AgentService::class),
)
class DownloadFilesTest {
    private val organization = Organization.stub(2).apply {
        name = "Example.com"
    }
    private val organization2 = Organization.stub(1).apply {
        name = "Huawei"
    }
    private var testProject: Project = Project.stub(3, organization).apply {
        name = "TheProject"
        url = "example.com"
        description = "This is an example project"
    }
    private var testProject2: Project = Project.stub(1, organization2).apply {
        name = "huaweiName"
        url = "huawei.com"
        description = "test description"
    }
    private var file1: File = File(
        project = testProject,
        name = "test-1.txt",
        uploadedTime = LocalDateTime.now(),
        sizeBytes = -1L,
        isExecutable = false,
    ).apply {
        id = 1L
    }
    private var file2: File = File(
        project = testProject2,
        name = "test-2.txt",
        uploadedTime = LocalDateTime.now(),
        sizeBytes = -1L,
        isExecutable = false,
    ).apply {
        id = 2L
    }

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var fileStorage: MigrationFileStorage

    @Autowired
    private lateinit var configProperties: ConfigProperties

    @MockBean
    private lateinit var projectService: ProjectService

    @MockBean
    private lateinit var fileRepository: FileRepository

    @MockBean
    private lateinit var projectPermissionEvaluator: ProjectPermissionEvaluator

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    @WithMockUser(roles = ["USER"])
    fun `should download a file`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 1)
        }

        mockFileRepository(file1)

        whenever(projectService.findByNameAndOrganizationNameAndCreatedStatus(eq(testProject.name), eq(organization.name)))
            .thenReturn(testProject)
        whenever(projectService.findWithPermissionByNameAndOrganization(any(), eq(testProject.name), eq(organization.name), eq(Permission.READ), anyOrNull(), any()))
            .thenAnswer { Mono.just(testProject) }

        val tmpFile = (createTempDirectory() / file1.name).createFile()
            .writeLines("Lorem ipsum".lines())
        Paths.get(configProperties.fileStorage.location).createDirectories()

        val projectCoordinates = ProjectCoordinates("Example.com", "TheProject")
        val sampleFileDto = tmpFile.toFileDto(projectCoordinates)
        fileStorage.overwrite(sampleFileDto, tmpFile.toDataBufferFlux().map { it.asByteBuffer() })
            .subscribeOn(Schedulers.immediate())
            .block()

        setOf(HttpMethod.GET, HttpMethod.POST)
            .forEach { httpMethod ->
                webTestClient.method(httpMethod)
                    .uri("/api/$v1/files/download?fileId={fileId}", sampleFileDto.id)
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody()
                    .consumeWith {
                        Assertions.assertArrayEquals("Lorem ipsum${System.lineSeparator()}".toByteArray(), it.responseBody)
                    }
            }

        webTestClient.get()
            .uri("/api/$v1/files/Example.com/TheProject/list")
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList<FileDto>()
            .hasSize(1)
            .consumeWith<WebTestClient.ListBodySpec<FileDto>> {
                Assertions.assertEquals(
                    tmpFile.name, it.responseBody!!.first().name
                )
                Assertions.assertTrue(
                    it.responseBody!!.first().sizeBytes > 0
                )
            }
    }

    @Test
    fun `should return 404 for non-existent files`() {
        webTestClient.get()
            .uri("/api/$v1/files/download/invalid-name")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Suppress("LongMethod")
    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun checkUpload() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 1)
        }

        mockFileRepository(file2)

        whenever(projectService.findByNameAndOrganizationNameAndCreatedStatus(eq(testProject2.name), eq(organization2.name)))
            .thenReturn(testProject2)
        whenever(projectService.findWithPermissionByNameAndOrganization(any(), eq(testProject2.name), eq(organization2.name), eq(Permission.WRITE), anyOrNull(), any()))
            .thenAnswer { Mono.just(testProject2) }

        val tmpFile = (createTempDirectory() / file2.name).createFile()
            .writeLines("Lorem ipsum".lines())

        val body = MultipartBodyBuilder().apply {
            part("file", FileSystemResource(tmpFile))
        }
            .build()

        webTestClient.post()
            .uri("/api/$v1/files/Huawei/huaweiName/upload")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(body))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<FileDto>()
            .consumeWith { result ->
                Assertions.assertTrue(
                    Flux.just(result.responseBody!!)
                        .flatMap { fileStorage.contentSize(it) }
                        .single()
                        .subscribeOn(Schedulers.immediate())
                        .toFuture()
                        .get() > 0
                )
            }
    }

    @Test
    fun `should save test data`() {
        val execution: Execution = mock()
        whenever(execution.id).thenReturn(1)

        webTestClient.post()
            .uri("/internal/files/debug-info?executionId=1")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                TestResultDebugInfo(
                    TestResultLocation("suite1", "plugin1", "path/to/test", "Test.test"),
                    DebugInfo("./a.out", "stdout", "stderr", 42L),
                    Pass(null),
                )
            )
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    fun `download save-agent`() {
        setOf(HttpMethod.GET, HttpMethod.POST)
            .forEach { httpMethod ->
                webTestClient.method(httpMethod)
                    .uri("/internal/files/download-save-agent")
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody()
                    .consumeWith {
                        Assertions.assertArrayEquals(
                            "content-save-agent.kexe".toByteArray(),
                            it.responseBody
                        )
                    }
            }
    }

    @Test
    fun `download save-cli`() {
        setOf(HttpMethod.GET, HttpMethod.POST)
            .forEach { httpMethod ->
                webTestClient.method(httpMethod)
                    .uri("/internal/files/download-save-cli?version=1.0")
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody()
                    .consumeWith {
                        Assertions.assertArrayEquals(
                            "content-save-cli.kexe".toByteArray(),
                            it.responseBody
                        )
                    }

                webTestClient.method(httpMethod)
                    .uri("/internal/files/download-save-cli?version=2.0")
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .exchange()
                    .expectStatus()
                    .isNotFound

            }
    }

    private fun mockFileRepository(file: File) {
        val saveCallCount = AtomicInteger()
        val isSaved: () -> Boolean = { saveCallCount.get() != 0 }
        whenever(fileRepository.save(any()))
            .thenAnswer {
                assert(saveCallCount.incrementAndGet() <= 2) {
                    "Too many times save() is called"
                }
                file
            }
        whenever(fileRepository.findById(file.requiredId()))
            .thenAnswer {
                if (isSaved()) {
                    Optional.of(file)
                } else {
                    Optional.empty()
                }
            }
        whenever(fileRepository.findAll())
            .thenAnswer {
                if (isSaved()) {
                    listOf(file)
                } else {
                    emptyList()
                }
            }
        whenever(fileRepository.findByProject_Organization_NameAndProject_NameAndNameAndUploadedTime(
            eq(file.project.organization.name),
            eq(file.project.name),
            eq(file.name),
            any()
        )).thenAnswer {
            if (isSaved()) {
                file
            } else {
                null
            }
        }
    }

    companion object {
        @TempDir internal lateinit var tmpDir: Path

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("backend.fileStorage.location") {
                tmpDir.absolutePathString()
            }
        }
    }
}
