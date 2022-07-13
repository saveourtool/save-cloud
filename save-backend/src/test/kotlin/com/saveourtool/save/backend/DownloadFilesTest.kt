package com.saveourtool.save.backend

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.configs.NoopWebSecurityConfig
import com.saveourtool.save.backend.configs.WebConfig
import com.saveourtool.save.backend.controllers.DownloadFilesController
import com.saveourtool.save.backend.repository.*
import com.saveourtool.save.backend.security.ProjectPermissionEvaluator
import com.saveourtool.save.backend.service.OrganizationService
import com.saveourtool.save.backend.service.ProjectService
import com.saveourtool.save.backend.service.UserDetailsService
import com.saveourtool.save.backend.storage.AvatarStorage
import com.saveourtool.save.backend.storage.DebugInfoStorage
import com.saveourtool.save.backend.storage.ExecutionInfoStorage
import com.saveourtool.save.backend.storage.FileStorage
import com.saveourtool.save.backend.utils.AuthenticationDetails
import com.saveourtool.save.backend.utils.mutateMockedUser
import com.saveourtool.save.core.result.DebugInfo
import com.saveourtool.save.core.result.Pass
import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.domain.ShortFileInfo
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.domain.TestResultLocation
import com.saveourtool.save.entities.Agent
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.OrganizationStatus
import com.saveourtool.save.entities.Project
import com.saveourtool.save.entities.ProjectStatus
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.v1

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
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
import reactor.core.publisher.Mono

import java.nio.file.Path
import java.nio.file.Paths

import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempFile
import kotlin.io.path.name
import kotlin.io.path.writeLines

@ActiveProfiles("test")
@WebFluxTest(controllers = [DownloadFilesController::class])
@Import(
    WebConfig::class,
    NoopWebSecurityConfig::class,
    TimestampBasedFileSystemRepository::class,
    TestDataFilesystemRepository::class,
    FileStorage::class,
    AvatarStorage::class,
    DebugInfoStorage::class,
    ExecutionInfoStorage::class,
)
@AutoConfigureWebTestClient
@EnableConfigurationProperties(ConfigProperties::class)
@MockBeans(
    MockBean(OrganizationService::class),
    MockBean(UserDetailsService::class),
)
class DownloadFilesTest {
    private val organization = Organization("Example.com", OrganizationStatus.CREATED, 1, null).apply { id = 2 }
    private val organization2 = Organization("Huawei", OrganizationStatus.CREATED, 1, null).apply { id = 1 }
    private var testProject: Project = Project(
        organization = organization,
        name = "The Project",
        url = "example.com",
        description = "This is an example project",
        status = ProjectStatus.CREATED,
        userId = 2,
    ).apply {
        id = 3
    }
    private var testProject2: Project = Project(
        organization = organization2,
        name = "huaweiName",
        url = "huawei.com",
        description = "test description",
        status = ProjectStatus.CREATED,
        userId = 1,
    ).apply {
        id = 1
    }

    @Autowired
    lateinit var webTestClient: WebTestClient
    
    @Autowired
    private lateinit var fileSystemRepository: TimestampBasedFileSystemRepository

    @Autowired
    private lateinit var dataFilesystemRepository: TestDataFilesystemRepository

    @Autowired
    private lateinit var configProperties: ConfigProperties

    @MockBean
    private lateinit var agentRepository: AgentRepository

    @MockBean
    private lateinit var projectService: ProjectService

    @MockBean
    private lateinit var projectPermissionEvaluator: ProjectPermissionEvaluator

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    @WithMockUser(roles = ["USER"])
    fun `should download a file`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 1)
        }

        whenever(projectService.findWithPermissionByNameAndOrganization(any(), eq(testProject.name), eq(organization.name), eq(Permission.READ), anyOrNull(), any()))
            .thenAnswer { Mono.just(testProject) }

        val tmpFile = createTempFile("test", "txt")
            .writeLines("Lorem ipsum".lines())
        Paths.get(configProperties.fileStorage.location).createDirectories()
        val sampleFileInfo = fileSystemRepository.saveFile(tmpFile, ProjectCoordinates("Example.com", "The Project"))

        webTestClient.method(HttpMethod.POST)
            .uri("/api/$v1/files/Example.com/The Project/download")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(sampleFileInfo)
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
                Assertions.assertArrayEquals("Lorem ipsum${System.lineSeparator()}".toByteArray(), it.responseBody)
            }

        webTestClient.get()
            .uri("/api/$v1/files/Example.com/The Project/list")
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList<FileInfo>()
            .hasSize(1)
            .consumeWith<WebTestClient.ListBodySpec<FileInfo>> {
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

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun checkUpload() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 1)
        }

        whenever(projectService.findWithPermissionByNameAndOrganization(any(), eq(testProject2.name), eq(organization2.name), eq(Permission.WRITE), anyOrNull(), any()))
            .thenAnswer { Mono.just(testProject2) }

        val tmpFile = createTempFile("test", "txt")
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
            .expectBody<ShortFileInfo>()
            .consumeWith {
                Assertions.assertTrue(
                    fileSystemRepository.getFileInfoByShortInfo(it.responseBody!!, ProjectCoordinates("Huawei", "huaweiName")).sizeBytes > 0
                )
            }
    }

    @Test
    fun `should save test data`() {
        val execution: Execution = mock()
        whenever(execution.id).thenReturn(1)
        whenever(agentRepository.findByContainerId("container-1"))
            .thenReturn(Agent("container-1", execution, "0.0.1"))

        webTestClient.post()
            .uri("/internal/files/debug-info?agentId=container-1")
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

    companion object {
        @JvmStatic private val logger = LoggerFactory.getLogger(DownloadFilesTest::class.java)
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
