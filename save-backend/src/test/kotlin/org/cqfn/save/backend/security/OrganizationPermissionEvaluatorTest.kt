package org.cqfn.save.backend.security

import org.cqfn.save.backend.repository.LnkUserOrganizationRepository
import org.cqfn.save.backend.service.LnkUserOrganizationService
import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.*
import org.cqfn.save.permission.Permission

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
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
@Import(OrganizationPermissionEvaluator::class, LnkUserOrganizationService::class)
class OrganizationPermissionEvaluatorTest {
    @Autowired private lateinit var organizationPermissionEvaluator: OrganizationPermissionEvaluator
    @MockBean private lateinit var lnkUserOrganizationRepository: LnkUserOrganizationRepository
    private lateinit var mockOrganization: Organization

    @BeforeEach
    fun setUp() {
        mockOrganization = Organization.stub(99)
    }

    @Test
    fun `default permissions for users with only global roles`() {
        userShouldHavePermissions(
            "super_admin", Role.SUPER_ADMIN, Role.NONE, *Permission.values()
        )
        userShouldHavePermissions(
            "admin", Role.ADMIN, Role.NONE, Permission.READ
        )
        userShouldHavePermissions(
            "owner", Role.OWNER, Role.NONE, Permission.READ
        )
        userShouldHavePermissions(
            "viewer", Role.VIEWER, Role.NONE, Permission.READ
        )
    }

    @Test
    fun `permissions for organization owners`() {
        mockOrganization.ownerId = 99
        userShouldHavePermissions(
            "super_admin", Role.SUPER_ADMIN, Role.OWNER, *Permission.values(), userId = 99
        )
        userShouldHavePermissions(
            "admin", Role.ADMIN, Role.OWNER, *Permission.values(), userId = 99
        )
        userShouldHavePermissions(
            "owner", Role.OWNER, Role.OWNER, *Permission.values(), userId = 99
        )
        userShouldHavePermissions(
            "viewer", Role.VIEWER, Role.OWNER, *Permission.values(), userId = 99
        )
    }

    @Test
    fun `permissions for organization admins`() {
        userShouldHavePermissions(
            "super_admin", Role.SUPER_ADMIN, Role.ADMIN, *Permission.values(), userId = 99
        )
        userShouldHavePermissions(
            "admin", Role.ADMIN, Role.ADMIN, Permission.READ, Permission.WRITE, userId = 99
        )
        userShouldHavePermissions(
            "owner", Role.OWNER, Role.ADMIN, Permission.READ, Permission.WRITE, userId = 99
        )
        userShouldHavePermissions(
            "viewer", Role.VIEWER, Role.ADMIN, Permission.READ, Permission.WRITE, userId = 99
        )
    }

    @Test
    fun `permissions for organization viewers`() {
        userShouldHavePermissions(
            "super_admin", Role.SUPER_ADMIN, Role.VIEWER, *Permission.values(), userId = 99
        )
        userShouldHavePermissions(
            "admin", Role.ADMIN, Role.VIEWER, Permission.READ, userId = 99
        )
        userShouldHavePermissions(
            "owner", Role.OWNER, Role.VIEWER, Permission.READ, userId = 99
        )
        userShouldHavePermissions(
            "viewer", Role.VIEWER, Role.VIEWER, Permission.READ, userId = 99
        )
    }

    private fun userShouldHavePermissions(
        username: String,
        role: Role,
        organizationRole: Role,
        vararg permissions: Permission,
        userId: Long = 1
    ) {
        val authentication = mockAuth(username, role.asSpringSecurityRole(), id = userId)
        whenever(lnkUserOrganizationRepository.findByUserIdAndOrganization(any(), any())).thenAnswer { invocation ->
            LnkUserOrganization(
                invocation.arguments[1] as Organization,
                mockUser((invocation.arguments[0] as Number).toLong()),
                organizationRole,
            )
        }
        permissions.forEach { permission ->
            Assertions.assertTrue(organizationPermissionEvaluator.hasPermission(authentication, mockOrganization, permission)) {
                "User by authentication=$authentication is expected to have permission $permission on project $mockOrganization"
            }
        }
        Permission.values().filterNot { it in permissions }.forEach { permission ->
            Assertions.assertFalse(organizationPermissionEvaluator.hasPermission(authentication, mockOrganization, permission)) {
                "User by authentication=$authentication isn't expected to have permission $permission on project $mockOrganization"
            }
        }
    }

    private fun mockAuth(principal: String, vararg roles: String, id: Long = 99) = UsernamePasswordAuthenticationToken(
        principal,
        "",
        roles.map { SimpleGrantedAuthority(it) }
    ).apply {
        details = AuthenticationDetails(id = id, identitySource = "")
    }

    private fun mockUser(id: Long) = User(null, null, null, "").apply { this.id = id }
}
