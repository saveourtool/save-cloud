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

import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
/**
 * Controller for processing links between users and their roles
 */
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
    @GetMapping("/projects/{organizationName}/{projectName}/users")
    fun getAllUsersByProjectNameAndOrganizationName(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        authentication: Authentication,
    ): List<UserInfo> {
        val project = projectService.findByNameAndOrganizationName(projectName, organizationName)
            ?: throw NoSuchElementException("There is no $projectName project in $organizationName organization")
        val usersWithRoles = if (projectPermissionEvaluator.hasPermission(authentication, project, Permission.READ)) {
            lnkUserProjectService.getAllUsersAndRolesByProject(project)
                .filter { (_, role) -> role != Role.NONE }
                .map { (user, role) ->
                    user.toUserInfo(mapOf(project.organization.name + "/" + project.name to role))
                }
                .also { logger.trace("Found ${it.size} users for project: $it") }
        } else {
            emptyList()
        }
        return usersWithRoles
    }
}
