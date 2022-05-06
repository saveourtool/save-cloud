package org.cqfn.save.backend.controller

import org.cqfn.save.backend.configs.WebSecurityConfig
import org.cqfn.save.backend.controllers.PermissionController
import org.cqfn.save.backend.repository.OrganizationRepository
import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.backend.security.ProjectPermissionEvaluator
import org.cqfn.save.backend.service.*
import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.backend.utils.ConvertingAuthenticationManager
import org.cqfn.save.backend.utils.mutateMockedUser
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Organization
import org.cqfn.save.entities.OrganizationStatus
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.User
import org.cqfn.save.permission.Permission
import org.cqfn.save.permission.SetRoleRequest
import org.cqfn.save.v1
import org.junit.jupiter.api.Test
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.core.publisher.Mono
import reactor.util.function.Tuples
import java.util.Optional

@WebFluxTest(controllers = [PermissionController::class])
@Import(
    WebSecurityConfig::class,
    OrganizationService::class,
    LnkUserProjectService::class,
    LnkUserOrganizationService::class,
    ConvertingAuthenticationManager::class,
    UserDetailsService::class,
)
@AutoConfigureWebTestClient
class PermissionControllerTest {
    @Autowired private lateinit var webTestClient: WebTestClient
    @MockBean private lateinit var permissionService: PermissionService
    @MockBean private lateinit var organizationRepository: OrganizationRepository
    @MockBean private lateinit var projectPermissionEvaluator: ProjectPermissionEvaluator
    @MockBean private lateinit var userRepository: UserRepository
    @MockBean private lateinit var projectService: ProjectService
    @MockBean private lateinit var lnkUserProjectService: LnkUserProjectService
    @MockBean private lateinit var lnkUserOrganizationService: LnkUserOrganizationService
    @MockBean private lateinit var organizationService: OrganizationService

    @Test
    @WithMockUser
    fun `should allow reading of roles if user has permission`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 99)
        }
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            project = Project.stub(id = 99),
            permission = Permission.READ,
        )
        given(permissionService.getRole(any(), any())).willReturn(Role.ADMIN)

        webTestClient.get()
            .uri("/api/$v1/projects/Huawei/huaweiName/users/roles?userName=admin")
            .exchange()
            .expectStatus().isOk
            .expectBody<Role>()
            .isEqualTo(Role.ADMIN)
        verify(permissionService, times(1)).getRole(any(), any())
    }

    @Test
    @WithMockUser
    fun `should forbid reading of roles if user doesn't have permission`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 99)
        }
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            project = Project.stub(id = 99),
            permission = null,
        )

        webTestClient.get()
            .uri("/api/$v1/projects/Huawei/huaweiName/users/roles?userName=admin")
            .exchange()
            .expectStatus().isNotFound
        verify(permissionService, times(0)).getRole(any(), any())
    }

    @Test
    @WithMockUser
    fun `should allow changing roles for organization owners`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 99)
        }
        given(userRepository.findByName(any())).willReturn(Optional.of(
            User("user", null, null, "").apply { id = 99 }
        ))
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            project = Project.stub(id = 99),
            permission = Permission.WRITE,
        )
        given(projectService.canChangeRoles(any(), any(), any(), any())).willReturn(true)
        given(organizationRepository.findByName(any())).willReturn(Organization("Example Org", OrganizationStatus.CREATED, ownerId = 99, null, null))
        given(permissionService.setRole(any(), any(), any())).willReturn(Mono.just(Unit))

        webTestClient.post()
            .uri("/api/$v1/projects/Huawei/huaweiName/users/roles")
            .bodyValue(SetRoleRequest("admin", Role.ADMIN))
            .exchange()
            .expectStatus().isOk
        verify(permissionService, times(1)).setRole(any(), any(), any())
    }

    @Test
    @WithMockUser
    fun `should forbid changing roles unless user is an organization owner`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 99)
        }
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            project = Project.stub(id = 99),
            permission = Permission.WRITE,
        )
        given(organizationRepository.findByName(any())).willReturn(Organization("Example Org", OrganizationStatus.CREATED, ownerId = 42, null, null))

        webTestClient.post()
            .uri("/api/$v1/projects/Huawei/huaweiName/users/roles")
            .bodyValue(SetRoleRequest("admin", Role.ADMIN))
            .exchange()
            .expectStatus().isForbidden
        verify(permissionService, times(0)).setRole(any(), any(), any())
    }

    @Test
    @WithMockUser
    fun `should return 404 when changing roles on hidden project`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 99)
        }
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            project = Project.stub(id = 99).apply { public = false },
            permission = null,
        )
        given(organizationRepository.findByName(any())).willReturn(Organization("Example Org", OrganizationStatus.CREATED, ownerId = 42, null, null))

        webTestClient.post()
            .uri("/api/$v1/projects/Huawei/huaweiName/users/roles")
            .bodyValue(SetRoleRequest("admin", Role.ADMIN))
            .exchange()
            .expectStatus().isNotFound
        verify(permissionService, times(0)).setRole(any(), any(), any())
    }

    @Test
    @WithMockUser
    fun `should get 404 when deleting users from project without permission`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 99)
        }
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            project = Project.stub(id = 99).apply { public = true },
            permission = null,
        )
        webTestClient.delete()
            .uri("/api/$v1/projects/Huawei/huaweiName/users/roles/user")
            .exchange()
            .expectStatus()
            .isNotFound
        verify(permissionService, times(0)).removeRole(any(), any(), any())
    }

    @Test
    @WithMockUser
    fun `should permit deleting users from project if user can change roles in project`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 99)
        }
        val project = Project.stub(id = 99)
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            project = project,
            permission = Permission.WRITE,
        )

        given(projectService.canChangeRoles(any(), any(), any(), any())).willReturn(true)
        given(permissionService.removeRole(any(), any(), any())).willReturn(Mono.just(Unit))
        webTestClient.delete()
            .uri("/api/$v1/projects/Huawei/huaweiName/users/roles/user")
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    @WithMockUser
    fun `should forbid removing people from project if user cannot change roles in project`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 99)
        }
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            project = Project.stub(id = 99),
            permission = Permission.WRITE,
        )
        given(projectService.canChangeRoles(any(), any(), any(), any())).willReturn(false)
        given(organizationService.canChangeRoles(any(), any(), any(), any())).willReturn(false)
        given(permissionService.removeRole(any(), any(), any())).willReturn(Mono.just(Unit))
        webTestClient.delete()
            .uri("/api/$v1/projects/Huawei/huaweiName/users/roles/user")
            .exchange()
            .expectStatus()
            .isForbidden
        verify(permissionService, times(0)).removeRole(any(), any(), any())
    }

    @Suppress("LAMBDA_IS_NOT_LAST_PARAMETER")
    private fun given(
        user: (InvocationOnMock) -> User,
        project: Project,
        permission: Permission?,
    ) {
        given(permissionService.findUserAndProject(any(), any(), any())).willAnswer { invocationOnMock ->
            Tuples.of(user(invocationOnMock), project).let { Mono.just(it) }
        }
        given(organizationService.findByName(any())).willReturn(project.organization)
        given(projectService.findByNameAndOrganizationName(any(), any())).willReturn(project)
        given(projectPermissionEvaluator.hasPermission(any(), any(), any())).willAnswer {
            when (it.arguments[2] as Permission?) {
                null -> false
                Permission.READ -> permission != null
                Permission.WRITE -> permission == Permission.WRITE || permission == Permission.DELETE
                Permission.DELETE -> permission == Permission.DELETE
            }
        }
        given(projectService.findUserByName(any())).willAnswer { invocationOnMock ->
            Optional.of(user(invocationOnMock))
        }
    }
}
