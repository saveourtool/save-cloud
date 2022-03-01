package org.cqfn.save.backend.controller

import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.repository.GitRepository
import org.cqfn.save.backend.repository.OrganizationRepository
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.scheduling.StandardSuitesUpdateScheduler
import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.backend.utils.mutateMockedUser
import org.cqfn.save.entities.*

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

@ActiveProfiles("secure")
@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
@MockBeans(
    MockBean(StandardSuitesUpdateScheduler::class),
)
@Suppress("UnsafeCallOnNullableType")
class ProjectControllerTest {
    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    private lateinit var gitRepository: GitRepository

    @Autowired
    lateinit var webClient: WebTestClient

    @Test
    @WithMockUser
    fun `should return all public projects`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 99)
        }

        webClient
            .get()
            .uri("/api/projects/not-deleted")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<List<Project>>()
            .consumeWith { exchangeResult ->
                val projects = exchangeResult.responseBody!!
                Assertions.assertTrue(projects.isNotEmpty())
                projects.forEach { Assertions.assertTrue(it.public) }
            }
    }

    @Test
    @WithUserDetails(value = "admin")
    fun `should return project based on name and owner`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 1)
        }

        getProjectAndAssert("huaweiName", "Huawei") {
            expectStatus()
                .isOk
                .expectBody<Project>()
                .consumeWith {
                    requireNotNull(it.responseBody)
                    Assertions.assertEquals(it.responseBody!!.url, "huawei.com")
                }
        }
    }

    @Test
    @WithMockUser(username = "Mr. Bruh", roles = ["VIEWER"])
    fun `should return 403 if user doesn't have write access`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 99)
        }

        getProjectAndAssert("huaweiName", "Huawei") {
            expectStatus().isForbidden
        }
    }

    @Test
    @WithMockUser(username = "Mr. Bruh", roles = ["VIEWER"])
    fun `should return 404 if user doesn't have access to a private project`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 99)
        }

        getProjectAndAssert("The Project", "Example.com") {
            expectStatus().isNotFound
        }
    }

    @Test
    @WithMockUser(username = "Tester", roles = ["VIEWER"])
    fun `check git from project`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 1)
        }

        val project = projectRepository.findById(1).get()
        webClient
            .post()
            .uri("/api/projects/git")
            .body(BodyInserters.fromValue(project))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GitDto>()
            .consumeWith {
                requireNotNull(it.responseBody)
                Assertions.assertEquals("github", it.responseBody!!.url)
            }
    }

    @Test
    @WithMockUser(username = "John Doe", roles = ["VIEWER"])
    fun `check save new project`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 2)
        }

        val gitDto = GitDto("qweqwe")
        // `project` references an existing user from test data
        val organization: Organization = organizationRepository.getOrganizationById(1)
        val project = Project("I", "Name", "uurl", ProjectStatus.CREATED, userId = 2, organization = organization)
        val newProject = NewProjectDto(
            project,
            gitDto,
        )

        saveProjectAndAssert(
            newProject,
            { expectStatus().isOk }
        ) {
            expectStatus()
                .isOk
                .expectBody<Project>()
                .consumeWith {
                    requireNotNull(it.responseBody)
                    Assertions.assertEquals(it.responseBody!!.url, project.url)
                }
        }

        Assertions.assertNotNull(gitRepository.findAll().find { it.url == gitDto.url })
    }

    @Test
    @WithMockUser
    fun `should forbid updating a project for a viewer`() {
        val project = Project.stub(99).apply {
            userId = 1
            organization = organizationRepository.findById(1).get()
        }
        projectRepository.save(project)
        mutateMockedUser {
            details = AuthenticationDetails(id = 3)
        }

        webClient.post()
            .uri("/api/projects/update")
            .bodyValue(project)
            .exchange()
            .expectStatus()
            .isForbidden
    }

    private fun getProjectAndAssert(name: String,
                                    organizationName: String,
                                    assertion: WebTestClient.ResponseSpec.() -> Unit
    ) = webClient
        .get()
        .uri("/api/projects/get/organization-name?name=$name&organizationName=$organizationName")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .let { assertion(it) }

    private fun saveProjectAndAssert(newProject: NewProjectDto,
                                     saveAssertion: WebTestClient.ResponseSpec.() -> Unit,
                                     getAssertion: WebTestClient.ResponseSpec.() -> Unit,
    ) {
        webClient
            .post()
            .uri("/api/projects/save")
            .body(BodyInserters.fromValue(newProject))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .let { saveAssertion(it) }

        val project = newProject.project
        webClient
            .get()
            .uri("/api/projects/get/organization-id?name=${project.name}&organizationId=${project.organization.id}")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .let { getAssertion(it) }
    }
}
