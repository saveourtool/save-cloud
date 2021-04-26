package org.cqfn.save.backend

import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.Project
import org.cqfn.save.repository.GitRepository

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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
        val project = Project("noname", "1", "1", "1", "1")
        val gitRepo = GitRepository("1")
        val executionRequest = ExecutionRequest(project, gitRepo)
        webClient.post()
            .uri("/submitExecutionRequest")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(executionRequest))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)  // because this post call preprocessor
        val projects = projectRepository.findAll()
        Assertions.assertTrue(projects.any { it.owner == project.owner })
    }

    @Test
    fun checkSaveExistingProject() {
        val project = Project("noname", "1", "1", "1", "1")
        val gitRepo = GitRepository("1")
        val executionRequest = ExecutionRequest(project, gitRepo)
        val executionsClones = listOf(executionRequest, executionRequest, executionRequest)
        executionsClones.forEach {
            webClient.post()
                .uri("/submitExecutionRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(it))
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        }
        val projects = projectRepository.findAll()
        Assertions.assertTrue(projects.size == 3)
    }
}
