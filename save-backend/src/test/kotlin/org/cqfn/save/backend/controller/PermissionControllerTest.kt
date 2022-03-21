package org.cqfn.save.backend.controller

import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.controllers.PermissionController
import org.cqfn.save.backend.security.ProjectPermissionEvaluator
import org.cqfn.save.backend.service.PermissionService
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.User
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
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
    @MockBean private lateinit var projectPermissionEvaluator: ProjectPermissionEvaluator

    @Test
    @WithMockUser
    fun `should allow reading of roles for public project`() {
        given(permissionService.findUserAndProject(any(), any(), any())).willAnswer { invocationOnMock ->
            Tuples.of(
                User(name = invocationOnMock.arguments[0] as String, null, null, ""),
                Project.stub(id = 99)
            ).let { Mono.just(it) }
        }
        given(projectPermissionEvaluator.hasPermission(any(), any(), any())).willReturn(true)
        given(permissionService.getRole(any(), any())).willReturn(Role.ADMIN)

        webTestClient.get()
            .uri("/api/projects/roles/Huawei/huaweiName?userName=admin")
            .exchange()
            .expectStatus().isOk
            .expectBody<Role>()
            .isEqualTo(Role.ADMIN)
    }

    @Test
    fun `should forbid reading of roles for private project for non-members`() {

    }

    @Test
    fun `should allow reading of roles for private project for members`() {

    }

    @Test
    fun `should allow changing roles for organization owners`() {

    }

    @Test
    fun `should forbid changing roles unless user is an organization owner`() {

    }
}