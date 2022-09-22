package com.saveourtool.save.backend.security

import com.saveourtool.save.backend.service.LnkOrganizationTestSuiteService
import com.saveourtool.save.backend.service.LnkUserOrganizationService
import com.saveourtool.save.backend.service.TestSuitesSourceService
import com.saveourtool.save.backend.service.UserDetailsService
import com.saveourtool.save.backend.utils.AuthenticationDetails
import com.saveourtool.save.core.utils.runIf
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.Project
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.permission.Rights
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

/**
 * Class that is capable of assessing organization's permissions regarding test suites.
 */
@Component
class TestSuitePermissionEvaluator {
    @Autowired
    private lateinit var lnkOrganizationTestSuiteService: LnkOrganizationTestSuiteService

    /**
     * @param organization
     * @param testSuite
     * @param permission
     * @return true if [organization] has required [permission] over [testSuite]
     */
    fun hasPermission(
        organization: Organization,
        testSuite: TestSuite,
        permission: Permission,
        authentication: Authentication?,
    ): Boolean = lnkOrganizationTestSuiteService.getRights(organization, testSuite).let { currentRights ->
        authentication?.hasRole(Role.SUPER_ADMIN) == true || when(permission) {
                Permission.READ -> testSuite.isPublic || canAccessTestSuite(currentRights)
                Permission.WRITE, Permission.DELETE -> canMaintainTestSuite(currentRights)
            }
        }

    private fun canMaintainTestSuite(rights: Rights) = rights == Rights.MAINTAIN
    private fun canAccessTestSuite(rights: Rights) = canMaintainTestSuite(rights) || rights == Rights.USE

    private fun Authentication.hasRole(role: Role): Boolean = authorities.any { it.authority == role.asSpringSecurityRole() }
}
