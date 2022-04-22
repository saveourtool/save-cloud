/**
 * Controller for processing links between users and their roles:
 * 1) to put new roles of users
 * 2) to get users and their roles by project
 */

package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.security.ProjectPermissionEvaluator
import org.cqfn.save.backend.service.LnkUserProjectService
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.domain.Role
import org.cqfn.save.info.UserInfo
import org.cqfn.save.permission.Permission
import org.cqfn.save.v1
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

/**
 * Controller for processing links between users and their roles
 */
@RestController
@RequestMapping("/api")
class LnkUserProjectController(
    private val lnkUserProjectService: LnkUserProjectService,
    private val projectService: ProjectService,
    private val projectPermissionEvaluator: ProjectPermissionEvaluator,
) {
    private val logger = LoggerFactory.getLogger(LnkUserProjectController::class.java)

    /**
     * @param organizationName
     * @param projectName
     * @param authentication
     * @return list of users with their roles, connected to the project
     * @throws NoSuchElementException
     */
    @GetMapping(path = ["/$v1/projects/{organizationName}/{projectName}/users"])
    fun getAllUsersByProjectNameAndOrganizationName(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        authentication: Authentication,
    ): List<UserInfo> {
        val project = projectService.findByNameAndOrganizationName(projectName, organizationName)
            ?: throw NoSuchElementException("There is no $projectName project in $organizationName organization")
        val usersWithRoles = if (projectPermissionEvaluator.hasPermission(authentication, project, Permission.READ)) {
            lnkUserProjectService.getAllUsersAndRolesByProject(project)
                .filter { (_, role) ->
                    role != Role.NONE
                }
                .map { (user, role) ->
                    user.toUserInfo(mapOf(project.organization.name + "/" + project.name to role))
                }
                .also {
                    logger.trace("Found ${it.size} users for project: $it")
                }
        } else {
            emptyList()
        }
        return usersWithRoles
    }

    /**
     * @param organizationName
     * @param projectName
     * @param authentication
     * @param prefix
     * @return list of users, not connected to the project
     * @throws NoSuchElementException
     * @throws ResponseStatusException
     */
    @GetMapping("/users/not-from/{organizationName}/{projectName}")
    @Suppress("UnsafeCallOnNullableType")
    fun getAllUsersNotFromProjectWithNamesStartingWith(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam prefix: String,
        authentication: Authentication,
    ): List<UserInfo> {
        if (prefix.isEmpty()) {
            return emptyList()
        }
        val project = projectService.findByNameAndOrganizationName(projectName, organizationName)
            ?: throw NoSuchElementException("There is no $projectName project in $organizationName organization")
        if (!projectPermissionEvaluator.hasPermission(authentication, project, Permission.READ)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        val projectUserIds = lnkUserProjectService.getAllUsersByProject(project).map { it.id!! }.toSet()
        // first we need to get users with exact match by name
        val exactMatchUsers = lnkUserProjectService.getNonProjectUsersByName(prefix, projectUserIds)
        // and then we need to get those whose names start with `prefix`
        val prefixUsers = lnkUserProjectService.getNonProjectUsersByNamePrefix(
            prefix,
            projectUserIds + exactMatchUsers.map { it.id!! },
            PAGE_SIZE - exactMatchUsers.size,
        )
        return (exactMatchUsers + prefixUsers).map { it.toUserInfo() }
    }
    companion object {
        const val PAGE_SIZE = 5
    }
}
