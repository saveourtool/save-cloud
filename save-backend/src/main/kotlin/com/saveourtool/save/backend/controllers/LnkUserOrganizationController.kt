/**
 * Controller for processing links between users and their roles in organizations:
 * 1) to put new roles of users
 * 2) to get users and their roles by organization
 * 3) to remove users from organizations
 */

package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.configs.ApiSwaggerSupport
import com.saveourtool.save.backend.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.save.backend.security.OrganizationPermissionEvaluator
import com.saveourtool.save.backend.service.LnkUserOrganizationService
import com.saveourtool.save.backend.service.OrganizationService
import com.saveourtool.save.backend.utils.AuthenticationDetails
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.OrganizationStatus
import com.saveourtool.save.entities.User
import com.saveourtool.save.info.OrganizationInfo
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.permission.SetRoleRequest
import com.saveourtool.save.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import reactor.util.function.Tuple2

import java.util.*

/**
 * Controller for processing links between users and their roles in organizations
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "api"),
    Tag(name = "roles"),
    Tag(name = "organizations"),
)
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
     */
    @GetMapping("{organizationName}/users")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get list of users that are connected with given organization.",
        description = "Get list of users that are connected with given organization.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true)
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched contest by it's name.")
    @ApiResponse(responseCode = "404", description = "Contest with such name was not found.")
    fun getAllUsersByOrganizationName(
        @PathVariable organizationName: String,
        authentication: Authentication,
    ): Mono<List<UserInfo>> = Mono.justOrEmpty(
        Optional.ofNullable(
            organizationService.findByName(organizationName)
        )
    )
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "No such organization was found."))
        }
        .map {
            lnkUserOrganizationService.getAllUsersAndRolesByOrganization(it)
        }
        .map { mapOfPermissions ->
            mapOfPermissions.filter { it.value != Role.NONE }.map { (user, role) ->
                user.toUserInfo(organizations = mapOf(organizationName to role))
            }
        }

    /**
     * @param organizationName
     * @param userName
     * @param authentication
     * @return role of user with [userName] in organization with [organizationName]
     */
    @GetMapping("/{organizationName}/users/roles")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get user's role in organization with given name.",
        description = "If userName is not present, then will return the role of current user in given organization, " +
                "otherwise will return role of user with name userName in organization with name organizationName.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "userName", `in` = ParameterIn.QUERY, description = "name of a user", required = false)
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched user's role.")
    @ApiResponse(responseCode = "403", description = "You are not allowed to see requested user's role.")
    @ApiResponse(responseCode = "404", description = "Requested user or organization doesn't exist.")
    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    fun getRole(
        @PathVariable organizationName: String,
        @RequestParam(required = false) userName: String?,
        authentication: Authentication,
    ): Mono<Role> = Mono.justOrEmpty(
        Optional.ofNullable(
            organizationService.findByName(organizationName)
        )
    )
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }
        .filter {
            organizationPermissionEvaluator.hasPermission(authentication, it, Permission.READ)
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN))
        }
        .flatMap<Tuple2<User, Organization>> { organization ->
            userName?.let { userName ->
                Mono.zip(
                    Mono.justOrEmpty(lnkUserOrganizationService.getUserByName(userName)),
                    Mono.just(organization)
                )
            } ?: Mono.zip(
                Mono.justOrEmpty(lnkUserOrganizationService.getUserById((authentication.details as AuthenticationDetails).id)),
                Mono.just(organization),
            )
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }
        .map { (user, organization) ->
            lnkUserOrganizationService.getRole(user, organization)
        }

    /**
     * Set role in [organizationName] according to [setRoleRequest]
     *
     * @param organizationName
     * @param setRoleRequest
     * @param authentication
     * @return string with response
     */
    @PostMapping("{organizationName}/users/roles")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Set user's role in organization with given name.",
        description = "Set user's role in organization with given name.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "setRoleRequest", `in` = ParameterIn.DEFAULT, description = "pair of userName and role that is requested to be set", required = true)
    )
    @ApiResponse(responseCode = "200", description = "Permission added")
    @ApiResponse(responseCode = "403", description = "User doesn't have permissions to manage this members")
    @ApiResponse(responseCode = "404", description = "Requested user or organization doesn't exist")
    @Suppress("TYPE_ALIAS")
    fun setRole(
        @PathVariable organizationName: String,
        @RequestBody setRoleRequest: SetRoleRequest,
        authentication: Authentication,
    ): Mono<ResponseEntity<String>> = Mono.justOrEmpty(
        Optional.ofNullable(
            organizationService.findByName(organizationName)
        )
    )
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }
        .filter {
            organizationPermissionEvaluator.hasPermission(authentication, it, Permission.WRITE)
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN))
        }
        .flatMap {
            Mono.zip(
                Mono.justOrEmpty(lnkUserOrganizationService.getUserByName(setRoleRequest.userName)),
                Mono.just(it),
            )
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN))
        }
        .filter { (user, organization) ->
            organizationPermissionEvaluator.canChangeRoles(organization, authentication, user, setRoleRequest.role)
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN))
        }
        .map { (user, organization) ->
            lnkUserOrganizationService.setRole(user, organization, setRoleRequest.role)
            ResponseEntity.ok(
                "Successfully set role ${setRoleRequest.role} to user ${user.name} in organization ${organization.name}"
            )
        }

    /**
     * Remove user named [userName] from organization with name [organizationName]
     *
     * @param organizationName
     * @param userName
     * @param authentication
     * @return string with response
     */
    @DeleteMapping("{organizationName}/users/roles/{userName}")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "DELETE",
        summary = "Set user's role in organization with given name.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "userName", `in` = ParameterIn.PATH, description = "name of user whose role is requested to be removed", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Role removed")
    @ApiResponse(responseCode = "403", description = "User doesn't have permissions to manage this members")
    @ApiResponse(responseCode = "404", description = "Requested user or organization doesn't exist")
    @Suppress("ThrowsCount", "TYPE_ALIAS")
    fun removeRole(
        @PathVariable organizationName: String,
        @PathVariable userName: String,
        authentication: Authentication,
    ): Mono<ResponseEntity<String>> = Mono.justOrEmpty(
        Optional.ofNullable(
            organizationService.findByName(organizationName)
        )
    )
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }
        .filter {
            organizationPermissionEvaluator.hasPermission(authentication, it, Permission.WRITE)
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN))
        }
        .flatMap {
            Mono.zip(
                Mono.justOrEmpty(lnkUserOrganizationService.getUserByName(userName)),
                Mono.just(it)
            )
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }
        .filter { (user, organization) ->
            organizationPermissionEvaluator.canChangeRoles(organization, authentication, user)
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN))
        }
        .map { (user, organization) ->
            lnkUserOrganizationService.removeRole(user, organization)
            ResponseEntity.ok("Successfully removed role of user ${user.name} in organization ${organization.name}")
        }

    /**
     * @param organizationName
     * @param prefix
     * @param authentication
     * @return list of users, not connected to the organization
     * @throws ResponseStatusException
     */
    @GetMapping("{organizationName}/users/not-from")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get all users not from organization with names starting with a given prefix.",
        description = "Get all users not connected with organization with name organizationName whose names start with the same prefix."
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "prefix", `in` = ParameterIn.QUERY, description = "prefix of username", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched list of users")
    @ApiResponse(responseCode = "404", description = "Requested organization doesn't exist")
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
                user.requiredId()
            }.toSet()
        // first we need to get users with exact match by name
        val exactMatchUsers = lnkUserOrganizationService.getNonOrganizationUsersByName(prefix, organizationUserIds)
        // and then we need to get those whose names start with `prefix`
        val prefixUsers = lnkUserOrganizationService.getNonOrganizationUsersByNamePrefix(
            prefix,
            organizationUserIds + exactMatchUsers.map { it.requiredId() },
            PAGE_SIZE - exactMatchUsers.size,
        )
        return (exactMatchUsers + prefixUsers).map { it.toUserInfo() }
    }

    /**
     * @param authentication
     * @return list of organizations that can create contests where current user has owner role
     */
    @GetMapping("/can-create-contests")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get all user's organizations that can create contests.",
        description = "Get all organizations that can create contests where user is a member."
    )
    @ApiResponse(responseCode = "200", description = "Role removed")
    @ApiResponse(responseCode = "403", description = "User doesn't have permissions to manage this members")
    @ApiResponse(responseCode = "404", description = "Requested user or organization doesn't exist")
    fun getAllUsersOrganizationsThatCanCreateContests(
        authentication: Authentication,
    ): Flux<Organization> = Flux.fromIterable(
        lnkUserOrganizationService.getSuperOrganizationsWithRole((authentication.details as AuthenticationDetails).id)
    )

    /**
     * Get not deleted organizations that are connected with current user.
     *
     * @param authentication
     * @return Map where key is organization name, value is a pair of avatar and role.
     */
    @GetMapping("/by-user/not-deleted")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get user's organizations.",
        description = "Get not deleted organizations where user is a member and his roles in them."
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched organization infos.")
    @ApiResponse(responseCode = "404", description = "Could not find user with this id.")
    @Suppress("UnsafeCallOnNullableType")
    fun getOrganizationWithRoles(
        authentication: Authentication,
    ): Flux<OrganizationInfo> = Mono.justOrEmpty(
        lnkUserOrganizationService.getUserById((authentication.details as AuthenticationDetails).id)
    )
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }
        .flatMapMany {
            Flux.fromIterable(lnkUserOrganizationService.getOrganizationsAndRolesByUser(it))
        }
        .filter {
            it.organization != null && it.organization?.status != OrganizationStatus.DELETED
        }
        .map {
            it.organization!!.toOrganizationInfo(mapOf(it.user.name!! to (it.role ?: Role.NONE)))
        }

    companion object {
        const val PAGE_SIZE = 5
    }
}
