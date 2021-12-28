package org.cqfn.save.backend.controller

import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.repository.GitRepository
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.scheduling.StandardSuitesUpdateScheduler
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.entities.GitDto
import org.cqfn.save.entities.NewProjectDto
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.ProjectStatus

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
@MockBeans(
    MockBean(StandardSuitesUpdateScheduler::class),
)
class ProjectControllerTest {
    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Autowired
    private lateinit var gitRepository: GitRepository

    @Autowired
    lateinit var webClient: WebTestClient

    @Test
    fun `should return all projects`() {
        webClient
            .get()
            .uri("/api/projects")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ParameterizedTypeReference.forType<List<Project>>(List::class.java))
            .value<Nothing> {
                Assertions.assertTrue(it.isNotEmpty())
            }
    }

    @Test
    @Suppress("UnsafeCallOnNullableType")
    fun `should return project based on name and owner`() {
        webClient
            .get()
            .uri("/api/getProject?name=huaweiName&owner=Huawei")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<Project>()
            .consumeWith {
                requireNotNull(it.responseBody)
                Assertions.assertEquals(it.responseBody!!.url, "huawei.com")
            }
    }

    @Test
    @Suppress("UnsafeCallOnNullableType", "TOO_MANY_LINES_IN_LAMBDA")
    fun `check git from project`() {
        projectRepository.findById(1).ifPresent {
            webClient
                .post()
                .uri("/api/getGit")
                .body(BodyInserters.fromValue(it))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<GitDto>()
                .consumeWith {
                    requireNotNull(it.responseBody)
                    Assertions.assertEquals(it.responseBody!!.url, "github")
                }
        }
    }

    @Test
    @Suppress("UnsafeCallOnNullableType", "TOO_MANY_LINES_IN_LAMBDA")
    fun `check save new project`() {
        val gitDto = GitDto("qweqwe")
        // `project` references an existing user from test data
        val project = Project("I", "Name", "uurl", "nullsss", ProjectStatus.CREATED, userId = 2, adminIds = null)
        val newProject = NewProjectDto(
            project,
            gitDto,
            "John Doe",
        )
        webClient
            .post()
            .uri("/api/saveProject")
            .body(BodyInserters.fromValue(newProject))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk

        webClient
            .get()
            .uri("/api/getProject?name=${project.name}&owner=${project.owner}")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<Project>()
            .consumeWith {
                requireNotNull(it.responseBody)
                Assertions.assertEquals(it.responseBody!!.url, project.url)
            }
        Assertions.assertNotNull(gitRepository.findAll().find { it.url == gitDto.url })
    }
}
