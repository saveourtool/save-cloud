package com.saveourtool.save.backend.controller

import com.saveourtool.save.backend.configs.NoopWebSecurityConfig
import com.saveourtool.save.backend.configs.WebConfig
import com.saveourtool.save.backend.controllers.OrganizationController
import com.saveourtool.save.backend.repository.*
import com.saveourtool.save.backend.security.OrganizationPermissionEvaluator
import com.saveourtool.save.backend.security.ProjectPermissionEvaluator
import com.saveourtool.save.backend.service.*
import com.saveourtool.save.backend.storage.TestSuitesSourceSnapshotStorage
import com.saveourtool.save.backend.utils.AuthenticationDetails
import com.saveourtool.save.backend.utils.mutateMockedUser
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.*
import com.saveourtool.save.v1
import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

import java.time.LocalDateTime
import java.util.*

@ActiveProfiles("test")
@WebFluxTest(controllers = [OrganizationController::class])
@Import(
    OrganizationService::class,
    OrganizationPermissionEvaluator::class,
    LnkUserOrganizationService::class,
    UserDetailsService::class,
    NoopWebSecurityConfig::class,
    GitService::class,
    TestSuitesSourceService::class,
    TestSuitesService::class,
    ExecutionService::class,
    AgentStatusService::class,
    AgentService::class,
    WebConfig::class,
    ProjectService::class,
    ProjectPermissionEvaluator::class,
    LnkUserProjectService::class,
    UserDetailsService::class,
    OriginalLoginRepository::class,
)
@AutoConfigureWebTestClient
@Suppress("UnsafeCallOnNullableType")
class OrganizationControllerTest {
    private val organization = Organization(
        "OrgForTests",
        OrganizationStatus.CREATED,
        dateCreated = LocalDateTime.now(),
        ownerId = 1
    ).also { it.id = 1 }
    private val adminUser = User(
        "admin",
        "",
        Role.VIEWER.toString(),
        ""
    ).also { it.id = 1 }
    private val johnDoeUser = User(
        "JohnDoe",
        "",
        Role.VIEWER.toString(),
        ""
    ).also { it.id = 2 }

    @MockBean
    private lateinit var organizationRepository: OrganizationRepository

    @MockBean
    private lateinit var lnkUserOrganizationRepository: LnkUserOrganizationRepository

    @Autowired
    private lateinit var webClient: WebTestClient

    @MockBean
    private lateinit var userRepository: UserRepository

    @MockBean
    private lateinit var gitRepository: GitRepository

    @MockBean
    private lateinit var testSuitesSourceRepository: TestSuitesSourceRepository

    @MockBean
    private lateinit var testSuiteRepository: TestSuiteRepository

    @MockBean
    private lateinit var testRepository: TestRepository

    @MockBean
    private lateinit var testExecutionRepository: TestExecutionRepository

    @MockBean
    private lateinit var testSuitesSourceSnapshotStorage: TestSuitesSourceSnapshotStorage

    @MockBean
    private lateinit var executionRepository: ExecutionRepository

    @MockBean
    private lateinit var agentStatusRepository: AgentStatusRepository

    @MockBean
    private lateinit var agentRepository: AgentRepository

    @MockBean
    private lateinit var projectRepository: ProjectRepository

    @MockBean
    private lateinit var lnkUserProjectRepository: LnkUserProjectRepository

    @BeforeEach
    internal fun setUp() {
        whenever(gitRepository.save(any())).then {
            it.arguments[0] as Git
        }
    }

    @Test
    @WithMockUser(value = "admin", roles = ["VIEWER"])
    fun `delete organization with owner permission`() {
        mutateMockedUserAndLink(organization, adminUser, Role.OWNER)
        webClient.delete()
            .uri("/api/$v1/organization/${organization.name}/delete")
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    @WithMockUser(value = "JohDoe", roles = ["VIEWER"])
    fun `delete organization without owner permission`() {
        mutateMockedUserAndLink(organization, johnDoeUser, Role.VIEWER)
        webClient.delete()
            .uri("/api/$v1/organization/${organization.name}/delete")
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    @WithMockUser(value = "admin", roles = ["VIEWER"])
    fun `list git credential in organization`() {
        mutateMockedUserAndLink(organization, adminUser, Role.OWNER)

        val git1 = Git("url1", null, null, organization)
        val git2 = Git("url2", null, null, organization)
        given(gitRepository.findAllByOrganizationId(organization.requiredId())).willReturn(listOf(git1, git2))
        webClient.get()
            .uri("/api/$v1/organizations/${organization.name}/list-git")
            .exchange()
            .also {
                it.expectBodyList(GitDto::class.java)
                    .hasSize(2)
                    .contains(GitDto(git1.url))
                    .contains(GitDto(git2.url))
            }
            .expectStatus()
            .isOk
    }

    @Test
    @WithMockUser(value = "admin", roles = ["VIEWER"])
    fun `upsert git credential in organization`() {
        mutateMockedUserAndLink(organization, adminUser, Role.OWNER)
        val gitDtoToCreate = GitDto("url")
        webClient.post()
            .uri("/api/$v1/organizations/${organization.name}/upsert-git")
            .bodyValue(gitDtoToCreate)
            .exchange()
            .expectStatus()
            .isOk
        verify(gitRepository, times(1)).save(argThat { isEqualToDto(gitDtoToCreate) })

        val gitExisted = Git(
            url = gitDtoToCreate.url,
            username = gitDtoToCreate.username,
            password = gitDtoToCreate.password,
            organization = organization
        )
        val gitDtoToUpdate = gitDtoToCreate.copy(username = "updated", password = "updated")
        given(gitRepository.findByOrganizationAndUrl(organization, gitExisted.url)).willReturn(gitExisted)
        webClient.post()
            .uri("/api/$v1/organizations/${organization.name}/upsert-git")
            .bodyValue(gitDtoToUpdate)
            .exchange()
            .expectStatus()
            .isOk
        verify(gitRepository, times(1)).save(argThat { isEqualToDto(gitDtoToUpdate) })
    }

    @Test
    @WithMockUser(value = "admin", roles = ["VIEWER"])
    fun `delete git credential in organization`() {
        mutateMockedUserAndLink(organization, adminUser, Role.OWNER)

        val gitExisted = Git(
            url = "url",
            organization = organization
        )
        given(gitRepository.findByOrganizationAndUrl(organization, gitExisted.url)).willReturn(gitExisted)
        webClient.delete()
            .uri("/api/$v1/organizations/${organization.name}/delete-git?url=${gitExisted.url}")
            .exchange()
            .expectStatus()
            .isOk
        verify(gitRepository, times(1)).delete(gitExisted)
    }

    private fun Git.isEqualToDto(gitDto: GitDto) =
            gitDto.url == url && gitDto.username == password && gitDto.password == password

    private fun mutateMockedUserAndLink(organization: Organization, user: User, userRole: Role) {
        mutateMockedUser {
            details = AuthenticationDetails(id = user.requiredId())
        }
        prepareLink(organization, user, userRole)
    }

    private fun prepareLink(organization: Organization, user: User, userRole: Role) {
        given(lnkUserOrganizationRepository.findByUserIdAndOrganizationName(any(), any())).willReturn(
            LnkUserOrganization(organization, user, userRole)
        )
        given(lnkUserOrganizationRepository.findByUserIdAndOrganization(any(), any())).willReturn(
            LnkUserOrganization(organization, user, userRole)
        )
        given(organizationRepository.findByName(any())).willReturn(organization)
        whenever(organizationRepository.save(any())).thenReturn(organization)
        given(userRepository.findByName(any())).willReturn(user)
        given(userRepository.findByNameAndSource(any(), any())).willReturn(user)
    }
}
