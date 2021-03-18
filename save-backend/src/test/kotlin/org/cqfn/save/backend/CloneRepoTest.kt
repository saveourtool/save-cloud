package org.cqfn.save.backend

import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.utils.DatabaseTestBase
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.ProjectDto
import org.cqfn.save.repository.GitRepository

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
class CloneRepoTest: DatabaseTestBase() {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Test
    fun checkSave() {
        val project = Project("noname","1","1","1","1")
        val gitRepo = GitRepository("1")
        val projectDto = ProjectDto(project, gitRepo)
        webClient.post()
            .uri("/cloneRepository")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(projectDto))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)  // because this post call preprocessor
        val projects = projectRepository.findAll()
        Assertions.assertTrue(projects.any { it.owner == project.owner })
    }
}
