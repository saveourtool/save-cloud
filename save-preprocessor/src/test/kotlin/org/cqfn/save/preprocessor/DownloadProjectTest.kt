package org.cqfn.save.preprocessor

import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.Project
import org.cqfn.save.preprocessor.config.ConfigProperties
import org.cqfn.save.preprocessor.controllers.DownloadProject
import org.cqfn.save.preprocessor.utils.RepositoryVolume
import org.cqfn.save.repository.GitRepository

import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

import java.io.File
import java.time.Duration

import kotlin.io.path.ExperimentalPathApi

@ExperimentalPathApi
@WebFluxTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownloadProjectTest(
    @Autowired private val webClient: WebTestClient,
    @Autowired private val configProperties: ConfigProperties,
) : RepositoryVolume() {
    lateinit var mockServerBackend: MockWebServer
    lateinit var mockServerOrchestrator: MockWebServer
    lateinit var downloadProjectController: DownloadProject

    @BeforeAll
    fun startServer() {
        mockServerBackend = MockWebServer()
        mockServerBackend.start(8000)
        mockServerOrchestrator = MockWebServer()
        mockServerOrchestrator.start(8001)
    }

    @AfterAll
    fun stopServer() {
        mockServerBackend.shutdown()
        mockServerOrchestrator.shutdown()
    }

    @BeforeEach
    fun webClientSetUp() {
        webClient.mutate().responseTimeout(Duration.ofSeconds(2)).build()
    }

    @BeforeEach
    fun initialize() {
        // Fixme: This may be used in the future
        val backendUrl = "http://localhost:${mockServerBackend.port}"
        val orchestratorUrl = "http://localhost:${mockServerOrchestrator.port}"
        downloadProjectController = DownloadProject(ConfigProperties("/home/cnb/repositories/", backendUrl, orchestratorUrl))
    }

    @Test
    fun testBadRequest() {
        val wrongRepo = GitRepository("wrongGit")
        val project = Project("owner", "someName", "type", wrongRepo.url, "descr")
        val request = ExecutionRequest(project, wrongRepo)
        webClient.post()
            .uri("/upload")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.ACCEPTED)
        Thread.sleep(2000)  // Time for request to delete directory
        Assertions.assertFalse(File("${configProperties.repository}/${wrongRepo.url.hashCode()}").exists())
    }

    /**
     * This one covers logic of connecting to services
     */
    @Test
    fun testCorrectDownload() {
        val validRepo = GitRepository("https://github.com/cqfn/save-cloud.git")
        val project = Project("owner", "someName", "type", validRepo.url, "descr")
        val request = ExecutionRequest(project, validRepo)
        webClient.post()
            .uri("/upload")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus()
            .isAccepted
            .expectBody(String::class.java)
            .isEqualTo<Nothing>("Clone pending")
        Assertions.assertTrue(File("${configProperties.repository}/${validRepo.url.hashCode()}").exists())
    }

    @AfterEach
    fun removeTestDir() {
        File(configProperties.repository).deleteRecursively()
    }
}
