package org.cqfn.save.backend.controller

import org.cqfn.save.backend.controllers.PermissionController
import org.cqfn.save.backend.security.ProjectPermissionEvaluator
import org.cqfn.save.backend.service.OrganizationService
import org.cqfn.save.backend.service.PermissionService
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.User
import org.cqfn.save.permission.Permission
import org.cqfn.save.permission.SetRoleRequest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.core.publisher.Mono
import reactor.util.function.Tuples

@WebFluxTest(controllers = [PermissionController::class])
@AutoConfigureWebTestClient
class PermissionControllerTest {
    @Autowired private lateinit var webTestClient: WebTestClient
    @MockBean private lateinit var permissionService: PermissionService
    @MockBean private lateinit var organizationService: OrganizationService
    @MockBean private lateinit var projectPermissionEvaluator: ProjectPermissionEvaluator

    @Test
    @WithMockUser
    fun `should allow reading of roles if user has permission`() {
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            project = Project.stub(id = 99),
            permission = Permission.READ,
        )
        given(permissionService.getRole(any(), any())).willReturn(Role.ADMIN)

        webTestClient.get()
            .uri("/api/projects/roles/Huawei/huaweiName?userName=admin")
            .exchange()
            .expectStatus().isOk
            .expectBody<Role>()
            .isEqualTo(Role.ADMIN)
        verify(permissionService, times(1)).getRole(any(), any())
    }

    @Test
    @WithMockUser
    fun `should forbid reading of roles if user doesn't have permission`() {
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            project = Project.stub(id = 99),
            permission = null,
        )
        given(permissionService.getRole(any(), any())).willReturn(Role.ADMIN)

        webTestClient.get()
            .uri("/api/projects/roles/Huawei/huaweiName?userName=admin")
            .exchange()
            .expectStatus().isNotFound
        verify(permissionService, times(0)).getRole(any(), any())
    }

    @Test
    @WithMockUser
    @Disabled("TODO")
    fun `should allow changing roles for organization owners`() {
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            project = Project.stub(id = 99),
            permission = Permission.WRITE,
        )
        given(organizationService.canChangeRoles(any(), any())).willReturn(true)
        given(permissionService.addRole(any(), any(), any())).willReturn(Mono.just(Unit))

        webTestClient.post()
            .uri("/api/projects/roles/Huawei/huaweiName?userName=admin")
            .bodyValue(SetRoleRequest("admin", Role.ADMIN))
            .exchange()
            .expectStatus().isOk
        verify(permissionService.addRole(any(), any(), any()), times(1))
    }

    @Test
    @WithMockUser
    fun `should forbid changing roles unless user is an organization owner`() {
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            project = Project.stub(id = 99),
            permission = Permission.WRITE,
        )
        given(organizationService.canChangeRoles(any(), any())).willReturn(false)

        webTestClient.post()
            .uri("/api/projects/roles/Huawei/huaweiName?userName=admin")
            .bodyValue(SetRoleRequest("admin", Role.ADMIN))
            .exchange()
            .expectStatus().isForbidden
        verify(permissionService, times(0)).addRole(any(), any(), any())
    }

    private fun given(
        user: (InvocationOnMock) -> User,
        project: Project,
        permission: Permission?,
    ) {
        given(permissionService.findUserAndProject(any(), any(), any())).willAnswer { invocationOnMock ->
            Tuples.of(user(invocationOnMock), project).let { Mono.just(it) }
        }
        given(projectPermissionEvaluator.hasPermission(any(), any(), any())).willAnswer {
            when (it.arguments[2] as Permission) {
                Permission.READ -> permission != null
                Permission.WRITE -> permission == Permission.WRITE || permission == Permission.DELETE
                Permission.DELETE -> permission == Permission.DELETE
            }
        }
    }
}
