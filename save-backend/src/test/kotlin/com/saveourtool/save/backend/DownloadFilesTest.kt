package com.saveourtool.save.backend

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.configs.NoopWebSecurityConfig
import com.saveourtool.save.backend.configs.WebConfig
import com.saveourtool.save.backend.controllers.DownloadFilesController
import com.saveourtool.save.backend.repository.*
import com.saveourtool.save.backend.service.OrganizationService
import com.saveourtool.save.backend.service.UserDetailsService
import com.saveourtool.save.core.result.DebugInfo
import com.saveourtool.save.core.result.Pass
import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.domain.ShortFileInfo
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.domain.TestResultLocation
import com.saveourtool.save.entities.Agent
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.v1

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.web.reactive.function.BodyInserters

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
)
@AutoConfigureWebTestClient
@EnableConfigurationProperties(ConfigProperties::class)
@MockBeans(
    MockBean(OrganizationService::class),
    MockBean(UserDetailsService::class),
)
class DownloadFilesTest {
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

    @Test
    fun `should download a file`() {
        val tmpFile = createTempFile("test", "txt")
            .writeLines("Lorem ipsum".lines())
        Paths.get(configProperties.fileStorage.location).createDirectories()
        val sampleFileInfo = fileSystemRepository.saveFile(tmpFile, "Example.com", "The Project")

        webTestClient.method(HttpMethod.GET).uri("/api/$v1/files/download?organizationName=Example.com&projectName=The Project")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(sampleFileInfo)
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .exchange()
            .expectStatus().isOk
            .expectBody().consumeWith {
                Assertions.assertArrayEquals("Lorem ipsum${System.lineSeparator()}".toByteArray(), it.responseBody)
            }

        webTestClient.get().uri("/api/$v1/files/list")
            .exchange()
            .expectStatus().isOk
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
        webTestClient.get().uri("/api/$v1/files/download/invalid-name").exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun checkUpload() {
        val tmpFile = createTempFile("test", "txt")
            .writeLines("Lorem ipsum".lines())

        val body = MultipartBodyBuilder().apply {
            part("file", FileSystemResource(tmpFile))
        }
            .build()

        webTestClient.post().uri("/api/$v1/files/upload?organizationName=Huawei&projectName=huaweiName")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(body))
            .exchange()
            .expectStatus().isOk
            .expectBody<ShortFileInfo>()
            .consumeWith {
                Assertions.assertTrue(
                    fileSystemRepository.getFileInfoByShortInfo(it.responseBody!!, "Huawei", "huaweiName").sizeBytes > 0
                )
            }
    }

    @Test
    fun `should save test data`() {
        val execution: Execution = mock()
        whenever(execution.id).thenReturn(1)
        whenever(agentRepository.findByContainerId("container-1"))
            .thenReturn(Agent("container-1", execution, "0.0.1"))

        webTestClient.post().uri("/internal/files/debug-info?agentId=container-1")
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

        dataFilesystemRepository.root.toFile().walk().onEnter {
            println(it.absolutePath)
            true
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
