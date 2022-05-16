/**
 * Controller for processing links between users and their roles in organizations:
 * 1) to put new roles of users
 * 2) to get users and their roles by organization
 * 3) to remove users from organizations
 */

package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.security.OrganizationPermissionEvaluator
import org.cqfn.save.backend.service.LnkUserOrganizationService
import org.cqfn.save.backend.service.OrganizationService
import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.OrganizationStatus
import org.cqfn.save.info.UserInfo
import org.cqfn.save.permission.Permission
import org.cqfn.save.permission.SetRoleRequest
import org.cqfn.save.v1

import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

/**
 * Controller for processing links between users and their roles in organizations
 */
@RestController
@RequestMapping("/api/$v1/organizations/")
class LnkUserOrganizationController(
    private val lnkUserOrganizationService: LnkUserOrganizationService,
    private val organizationService: OrganizationService,
    private val organizationPermissionEvaluator: OrganizationPermissionEvaluator,
) {
    private val logger = LoggerFactory.getLogger(LnkUserOrganizationController::class.java)

    /**
     * @param organizationName
     * @param authentication
     * @return list of users with their roles, connected to the organization with [organizationName]
     * @throws ResponseStatusException
     */
    @GetMapping("{organizationName}/users")
    fun getAllUsersByOrganizationName(
        @PathVariable organizationName: String,
        authentication: Authentication,
    ): List<UserInfo> {
        val organization = organizationService.findByName(organizationName)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        val usersWithRoles = lnkUserOrganizationService.getAllUsersAndRolesByOrganization(organization)
            .filter { (_, role) -> role != Role.NONE }
            .map { (user, role) ->
                user.toUserInfo(organizations = mapOf(organization.name to role))
            }
            .also { logger.trace("Found ${it.size} users for organization: $it") }
        return usersWithRoles
    }

    /**
     * @param organizationName
     * @param userName
     * @param authentication
     * @return role of user with [userName] in organization with [organizationName]
     * @throws ResponseStatusException
     */
    @GetMapping("/{organizationName}/users/roles")
    @ApiResponse(responseCode = "200", description = "Successfully fetched user's role")
    @ApiResponse(
        responseCode = "404", description = "Requested user or organization doesn't exist."
    )
    fun getRole(
        @PathVariable organizationName: String,
        // fixme: userName should be like that: ${user.source}:${user.name}
        @RequestParam(required = false) userName: String?,
        authentication: Authentication,
    ): Role? {
        val selfId = (authentication.details as AuthenticationDetails).id
        val organization = organizationService.findByName(organizationName)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        if (!organizationPermissionEvaluator.hasPermission(authentication, organization, Permission.READ)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        val user = userName?.let { name ->
            lnkUserOrganizationService.getUserByName(name)
                .orElseThrow {
                    ResponseStatusException(HttpStatus.NOT_FOUND)
                }
        }
            ?: lnkUserOrganizationService.getUserById(selfId)
                .orElseThrow {
                    ResponseStatusException(HttpStatus.NOT_FOUND)
                }
        return lnkUserOrganizationService.getRole(user, organization)
    }

    /**
     * Set role in [organizationName] according to [setRoleRequest]
     *
     * @param organizationName
     * @param setRoleRequest
     * @param authentication
     * @throws ResponseStatusException
     */
    @PostMapping("{organizationName}/users/roles")
    @ApiResponse(responseCode = "200", description = "Permission added")
    @ApiResponse(responseCode = "403", description = "User doesn't have permissions to manage this members")
    @ApiResponse(responseCode = "404", description = "Requested user or organization doesn't exist")
    @Suppress("ThrowsCount")
    fun setRole(
        @PathVariable organizationName: String,
        @RequestBody setRoleRequest: SetRoleRequest,
        authentication: Authentication,
    ) {
        val organization = organizationService.findByName(organizationName)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        if (!organizationPermissionEvaluator.hasPermission(authentication, organization, Permission.WRITE)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        val user = lnkUserOrganizationService.getUserByName(setRoleRequest.userName)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND)
            }
        if (organizationPermissionEvaluator.canChangeRoles(organization, authentication, user, setRoleRequest.role)) {
            lnkUserOrganizationService.setRole(user, organization, setRoleRequest.role)
        } else {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
    }

    /**
     * Remove user named [userName] from organization with name [organizationName]
     *
     * @param organizationName
     * @param userName
     * @param authentication
     * @throws ResponseStatusException
     */
    @DeleteMapping("{organizationName}/users/roles/{userName}")
    @ApiResponse(responseCode = "200", description = "Permission removed")
    @ApiResponse(responseCode = "403", description = "User doesn't have permissions to manage this members")
    @ApiResponse(responseCode = "404", description = "Requested user or organization doesn't exist")
    @Suppress("ThrowsCount")
    fun removeRole(@PathVariable organizationName: String,
                   @PathVariable userName: String,
                   authentication: Authentication,
    ) {
        val organization = organizationService.findByName(organizationName)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        if (!organizationPermissionEvaluator.hasPermission(authentication, organization, Permission.WRITE)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        val user = lnkUserOrganizationService.getUserByName(userName)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        if (organizationPermissionEvaluator.canChangeRoles(organization, authentication, user)) {
            lnkUserOrganizationService.removeRole(user, organization)
        } else {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
    }

    /**
     * @param organizationName
     * @param prefix
     * @param authentication
     * @return list of users, not connected to the organization
     * @throws ResponseStatusException
     */
    @GetMapping("{organizationName}/users/not-from")
    @Suppress("UnsafeCallOnNullableType")
    fun getAllUsersNotFromOrganizationWithNamesStartingWith(
        @PathVariable organizationName: String,
        @RequestParam prefix: String,
        authentication: Authentication,
    ): List<UserInfo> {
        if (prefix.isEmpty()) {
            return emptyList()
        }
        val organization = organizationService.findByName(organizationName)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        val organizationUserIds = lnkUserOrganizationService.getAllUsersAndRolesByOrganization(organization)
            .map { (user, _) ->
                user.id!!
            }.toSet()
        // first we need to get users with exact match by name
        val exactMatchUsers = lnkUserOrganizationService.getNonOrganizationUsersByName(prefix, organizationUserIds)
        // and then we need to get those whose names start with `prefix`
        val prefixUsers = lnkUserOrganizationService.getNonOrganizationUsersByNamePrefix(
            prefix,
            organizationUserIds + exactMatchUsers.map { it.id!! },
            PAGE_SIZE - exactMatchUsers.size,
        )
        return (exactMatchUsers + prefixUsers).map { it.toUserInfo() }
    }

    /**
     * Get not deleted organizations that are connected with current user.
     *
     * @param authentication
     * @return Map where key is organization name, value is a pair of avatar and role.
     */
    @Suppress("TYPE_ALIAS", "UnsafeCallOnNullableType")
    @GetMapping("/by-user/not-deleted")
    fun getOrganizationWithRoles(
        authentication: Authentication,
    ): Map<String, Pair<String?, Role>> {
        val selfId = (authentication.details as AuthenticationDetails).id
        val user = lnkUserOrganizationService.getUserById(selfId).get()
        return lnkUserOrganizationService
            .getOrganizationsAndRolesByUser(user)
            .filter { it.organization?.status != OrganizationStatus.DELETED }
            .associate {
                (it.organization!!.name) to (it.organization!!.avatar to (it.role ?: Role.NONE))
            }
    }
    companion object {
        const val PAGE_SIZE = 5
    }
}
