package org.cqfn.save.backend

import org.cqfn.save.backend.repository.AgentStatusRepository
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.service.TestExecutionService

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

import kotlin.io.path.ExperimentalPathApi

@WebFluxTest
class DownloadFilesTest {
    @MockBean
    lateinit var repository: ProjectRepository

    @MockBean
    lateinit var agentStatusRepository: AgentStatusRepository

    @MockBean
    lateinit var testExecutionRepository: TestExecutionRepository

    @MockBean
    lateinit var testExecutionService: TestExecutionService

    @Autowired
    lateinit var webClient: WebTestClient

    @Test
    fun checkDownload() {
        webClient.get().uri("/download").exchange()
            .expectStatus().isOk
        webClient.get().uri("/download").exchange()
            .expectBody(String::class.java).isEqualTo<Nothing>("qweqwe")
    }

    @Test
    @ExperimentalPathApi
    fun checkUpload() {
        val tmpFile = kotlin.io.path.createTempFile("test", "txt")

        val body = MultipartBodyBuilder().apply {
            part("file", object : ByteArrayResource("testString".toByteArray()) {
                override fun getFilename() = tmpFile.fileName.toString()
            })
        }.build()

        webClient.post().uri("/upload").body(BodyInserters.fromMultipartData(body))
            .exchange().expectStatus().isOk

        webClient.post().uri("/upload").body(BodyInserters.fromMultipartData(body))
            .exchange().expectBody(String::class.java).isEqualTo<Nothing>("test")
    }
}
