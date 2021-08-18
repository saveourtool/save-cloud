package org.cqfn.save.backend

import org.cqfn.save.backend.controllers.DownloadFilesController
import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.backend.repository.AgentStatusRepository
import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.repository.GitRepository
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.backend.repository.TestSuiteRepository
import org.cqfn.save.backend.scheduling.StandardSuitesUpdateScheduler

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

import kotlin.io.path.ExperimentalPathApi

@WebFluxTest(controllers = [DownloadFilesController::class])
@AutoConfigureWebTestClient
@MockBeans(
    MockBean(AgentStatusRepository::class),
    MockBean(AgentRepository::class),
    MockBean(ExecutionRepository::class),
    MockBean(ProjectRepository::class),
    MockBean(TestExecutionRepository::class),
    MockBean(TestRepository::class),
    MockBean(TestSuiteRepository::class),
    MockBean(GitRepository::class),
    MockBean(StandardSuitesUpdateScheduler::class),
)
class DownloadFilesTest {
    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun checkDownload() {
        webTestClient.get().uri("/download").exchange()
            .expectStatus().isOk
        webTestClient.get().uri("/download").exchange()
            .expectBody<String>().isEqualTo("qweqwe")
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

        webTestClient.post().uri("/upload").body(BodyInserters.fromMultipartData(body))
            .exchange().expectStatus().isOk

        webTestClient.post().uri("/upload").body(BodyInserters.fromMultipartData(body))
            .exchange().expectBody<String>().isEqualTo("test")
    }
}
