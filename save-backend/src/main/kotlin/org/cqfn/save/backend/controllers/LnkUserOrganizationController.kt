/**
 * Controller for processing links between users and their roles in organizations:
 * 1) to put new roles of users
 * 2) to get users and their roles by organization
 * 3) to remove users from organizations
 */

package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.security.OrganizationPermissionEvaluator
import org.cqfn.save.backend.security.ProjectPermissionEvaluator
import org.cqfn.save.backend.service.LnkUserOrganizationService
import org.cqfn.save.backend.service.LnkUserProjectService
import org.cqfn.save.backend.service.OrganizationService
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.LnkUserOrganization
import org.cqfn.save.info.UserInfo
import org.cqfn.save.permission.Permission

import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

/**
 * Controller for processing links between users and their roles in organizations
 */
@RestController
@RequestMapping("/api")
class LnkUserOrganizationController(
    private val lnkUserOrganizationService: LnkUserOrganizationService,
    private val organizationService: OrganizationService,
    private val organizationPermissionEvaluator: OrganizationPermissionEvaluator,
) {
    private val logger = LoggerFactory.getLogger(LnkUserOrganizationController::class.java)

    /**
     * @param organizationName
     * @param authentication
     * @return list of users with their roles, connected to the organization
     * @throws NoSuchElementException
     */
    @GetMapping("/organizations/{organizationName}/users")
    fun getAllUsersByOrganizationName(
        @PathVariable organizationName: String,
        authentication: Authentication,
    ): List<UserInfo> {
        val organization = organizationService.findByName(organizationName)
            ?: throw NoSuchElementException("There is no $organizationName organization.")
        val usersWithRoles = lnkUserOrganizationService.getAllUsersAndRolesByOrganization(organization)
            .filter { (_, role) -> role != Role.NONE }
            .map { (user, role) ->
                user.toUserInfo(organizations = mapOf(organization.name to role))
            }
            .also { logger.trace("Found ${it.size} users for project: $it") }
        return usersWithRoles
    }
}
