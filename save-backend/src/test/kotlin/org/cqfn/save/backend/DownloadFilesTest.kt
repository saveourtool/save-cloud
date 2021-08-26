package org.cqfn.save.backend

import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.backend.controllers.DownloadFilesController
import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.backend.repository.AgentStatusRepository
import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.repository.FileSystemRepository
import org.cqfn.save.backend.repository.GitRepository
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.backend.repository.TestSuiteRepository
import org.junit.jupiter.api.Assertions

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import java.nio.file.Path
import java.nio.file.Paths

import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createTempFile
import kotlin.io.path.writeLines

@WebFluxTest(controllers = [DownloadFilesController::class])
@Import(FileSystemRepository::class)
@AutoConfigureWebTestClient
@EnableConfigurationProperties(ConfigProperties::class)
@MockBeans(
    MockBean(AgentStatusRepository::class),
    MockBean(AgentRepository::class),
    MockBean(ExecutionRepository::class),
    MockBean(ProjectRepository::class),
    MockBean(TestExecutionRepository::class),
    MockBean(TestRepository::class),
    MockBean(TestSuiteRepository::class),
    MockBean(GitRepository::class),
)
class DownloadFilesTest {
    @Autowired
    lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var configProperties: ConfigProperties

    @Test
    fun `should download a file`() {
        Paths.get(configProperties.fileStorage.location)
            .createDirectories()
            .resolve("sample-name")
            .createFile()
            .writeLines("Lorem ipsum".lines())

        webTestClient.get().uri("/files/download/sample-name")
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .exchange()
            .expectStatus().isOk
            .expectBody().consumeWith {
                Assertions.assertArrayEquals("Lorem ipsum${System.lineSeparator()}".toByteArray(), it.responseBody)
            }
    }

    @Test
    fun `should return 404 for non-existent files`() {
        webTestClient.get().uri("/files/download/invalid-name").exchange()
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

        webTestClient.post().uri("/files/upload")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(body))
            .exchange()
            .expectStatus().isOk
            .expectBody<String>().consumeWith {
                Assertions.assertLinesMatch(
                    listOf("Saved \\d+ bytes"),
                    listOf(it.responseBody)
                )
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
