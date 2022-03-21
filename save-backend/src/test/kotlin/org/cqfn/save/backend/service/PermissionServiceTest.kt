package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@Import(PermissionService::class)
@MockBeans(
    MockBean(ProjectService::class),
    MockBean(UserRepository::class),
    MockBean(LnkUserProjectService::class),
)
class PermissionServiceTest {
    @Autowired private lateinit var permissionService: PermissionService
    @Autowired private lateinit var lnkUserProjectService: LnkUserProjectService

    @Test
    fun `should return a role`() {
        given(lnkUserProjectService.findRoleByUserIdAndProject(eq(99), any())).willReturn(Role.ADMIN)

        val role = permissionService.getRole(User(name = "admin", null, null, "").apply { id = 99 }, Project.stub(id = 99))

        Assertions.assertEquals(Role.ADMIN, role)
    }
}