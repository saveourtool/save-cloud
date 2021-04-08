package org.cqfn.save.backend

import org.cqfn.save.backend.controllers.AgentsController
import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.backend.repository.AgentStatusRepository
import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.backend.repository.TestSuiteRepository
import org.cqfn.save.backend.service.ExecutionService
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.backend.service.TestExecutionService
import org.cqfn.save.backend.service.TestService
import org.cqfn.save.backend.service.TestSuitesService

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient

import kotlin.io.path.ExperimentalPathApi

@WebFluxTest
@AutoConfigureWebTestClient
class DownloadFilesTest {
    @MockBean
    lateinit var repository: ProjectRepository

    @MockBean
    lateinit var agentStatusRepository: AgentStatusRepository
    @MockBean private lateinit var agentRepository: AgentRepository
    @MockBean private lateinit var agentsController: AgentsController

    @MockBean
    lateinit var projectService: ProjectService

    @MockBean
    @Qualifier("preprocessorWebClient")
    lateinit var webClient: WebClient

    @MockBean
    lateinit var testExecutionRepository: TestExecutionRepository

    @MockBean
    lateinit var executionRepository: ExecutionRepository

    @MockBean
    lateinit var testRepository: TestRepository

    @MockBean
    lateinit var testSuiteRepository: TestSuiteRepository

    @MockBean
    lateinit var testExecutionService: TestExecutionService

    @MockBean
    lateinit var executionService: ExecutionService

    @MockBean
    lateinit var testService: TestService

    @MockBean
    lateinit var testSuitesService: TestSuitesService

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun checkDownload() {
        webTestClient.get().uri("/download").exchange()
            .expectStatus().isOk
        webTestClient.get().uri("/download").exchange()
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

        webTestClient.post().uri("/upload").body(BodyInserters.fromMultipartData(body))
            .exchange().expectStatus().isOk

        webTestClient.post().uri("/upload").body(BodyInserters.fromMultipartData(body))
            .exchange().expectBody(String::class.java).isEqualTo<Nothing>("test")
    }
}
