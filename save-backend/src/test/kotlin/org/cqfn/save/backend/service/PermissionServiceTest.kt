package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.User
import org.cqfn.save.permission.SetRoleRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.Optional

@ExtendWith(SpringExtension::class)
@Import(PermissionService::class)
@MockBeans(
    MockBean(ProjectService::class),
    MockBean(UserRepository::class),
    MockBean(LnkUserProjectService::class),
)
class PermissionServiceTest {
    @Autowired private lateinit var permissionService: PermissionService
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var projectService: ProjectService
    @Autowired private lateinit var lnkUserProjectService: LnkUserProjectService

    @Test
    fun `should return a role`() {
        given(userRepository.findByName(any())).willAnswer { invocationOnMock ->
            User(invocationOnMock.arguments[0] as String, null, null, "basic")
                .apply { id = 99 }
                .let { Optional.of(it) }
        }
        given(projectService.findByNameAndOrganizationName(any(), any())).willAnswer {
            Project.stub(id = 99)
        }
        given(lnkUserProjectService.findRoleByUserIdAndProject(eq(99), any())).willReturn(Role.ADMIN)

        val role = permissionService.getRole(userName = "admin", projectName = "Example", organizationName = "Example Org")
            .blockOptional()

        Assertions.assertEquals(Role.ADMIN, role.get())
    }

    @Test
    fun `should return empty for non-existent projects or users`() {
        given(userRepository.findByName(any())).willReturn(Optional.empty<User>())
        given(projectService.findByNameAndOrganizationName(any(), any())).willReturn(null)

        val role = permissionService.getRole(userName = "admin", projectName = "Example", organizationName = "Example Org")
            .blockOptional()

        Assertions.assertTrue(role.isEmpty)
        verify(lnkUserProjectService, times(0)).findRoleByUserIdAndProject(any(), any())
    }

    @Test
    fun `should add a role`() {
        given(userRepository.findByName(any())).willAnswer { invocationOnMock ->
            User(invocationOnMock.arguments[0] as String, null, null, "basic")
                .apply { id = 99 }
                .let { Optional.of(it) }
        }
        given(projectService.findByNameAndOrganizationName(any(), any())).willAnswer {
            Project.stub(id = 99)
        }

        val result = permissionService.addRole("Example Org", "Example", SetRoleRequest("user", Role.ADMIN))
            .blockOptional()

        Assertions.assertTrue(result.isPresent)
        verify(lnkUserProjectService, times(1)).addRole(any(), any(), any())
    }
}
