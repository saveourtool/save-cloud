package org.cqfn.save.backend.security

import org.cqfn.save.backend.repository.LnkUserProjectRepository
import org.cqfn.save.backend.service.LnkUserProjectService
import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Project
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class, MockitoExtension::class)
@Import(ProjectPermissionEvaluator::class, LnkUserProjectService::class)
class ProjectPermissionEvaluatorTest {
    @Autowired private lateinit var projectPermissionEvaluator: ProjectPermissionEvaluator
    @MockBean private lateinit var lnkUserProjectRepository: LnkUserProjectRepository
    private var mockProject = Project.stub(99)

    @Test
    fun `should allow any operations for SUPER_ADMIN`() {
        val auth = mockAuth("admin", Role.SUPER_ADMIN.asSpringSecurityRole())
        whenever(lnkUserProjectRepository.findByUserIdAndProject(any(), any())).thenReturn(emptyList())
        Assertions.assertTrue(projectPermissionEvaluator.hasPermission(auth, mockProject, Permission.READ))
        Assertions.assertTrue(projectPermissionEvaluator.hasPermission(auth, mockProject, Permission.WRITE))
        Assertions.assertTrue(projectPermissionEvaluator.hasPermission(auth, mockProject, Permission.DELETE))
    }

    private fun mockAuth(principal: String, vararg roles: String, id: Long = 99) = UsernamePasswordAuthenticationToken(
        principal,
        "",
        roles.map { SimpleGrantedAuthority(it) }
    ).apply {
        details = AuthenticationDetails(id = id, identitySource = "")
    }
}