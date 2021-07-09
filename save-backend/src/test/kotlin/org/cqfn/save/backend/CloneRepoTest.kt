package org.cqfn.save.backend

import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.GitDto
import org.cqfn.save.entities.Project

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
class CloneRepoTest {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Test
    fun checkSaveProject() {
        mockServerPreprocessor.enqueue(
            MockResponse()
                .setResponseCode(202)
                .setBody("Clone pending")
                .addHeader("Content-Type", "application/json")
        )
        val project = projectRepository.findAll().first()
        val gitRepo = GitDto("1")
        val executionRequest = ExecutionRequest(project, gitRepo)
        webClient.post()
            .uri("/submitExecutionRequest")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(executionRequest))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.ACCEPTED)
    }

    @Test
    fun checkNonExistingProject() {
        val project = Project("noname", "1", "1", "1")
        val gitRepo = GitDto("1")
        val executionRequest = ExecutionRequest(project, gitRepo)
        val executionsClones = listOf(executionRequest, executionRequest, executionRequest)
        executionsClones.forEach {
            webClient.post()
                .uri("/submitExecutionRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(it))
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    companion object {
        @JvmStatic lateinit var mockServerPreprocessor: MockWebServer

        @AfterAll
        fun tearDown() {
            mockServerPreprocessor.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            mockServerPreprocessor = MockWebServer()
            mockServerPreprocessor.start()
            registry.add("backend.preprocessorUrl") { "http://localhost:${mockServerPreprocessor.port}" }
        }
    }
}
