/**
 * Controller for processing links between users and their roles:
 * 1) to put new roles of users
 * 2) to get users and their roles by project
 */

package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.LnkUserProjectService
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.entities.UserDto

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/links")
/**
 * Controller for processing links between users and their roles
 */
class LnkUserProjectController(
    private val lnkUserProjectService: LnkUserProjectService,
    private val projectService: ProjectService,
) {
    private val logger = LoggerFactory.getLogger(LnkUserProjectController::class.java)

    /**
     * @param projectName
     * @param organizationName
     * @return list of users with their roles, connected to the project
     * @throws NoSuchElementException
     */
    @GetMapping("/projects/get-by-project")
    fun getAllUsersByProjectNameAndOrganizationName(@RequestParam projectName: String, @RequestParam organizationName: String): List<UserDto> {
        val project = projectService.findByNameAndOrganizationName(projectName, organizationName)
            ?: throw NoSuchElementException("There is no $projectName project in $organizationName organization")
        return lnkUserProjectService.getAllUsersAndRolesByProject(project).map { (user, role) ->
            user.toDto(mapOf(project.organization.name + "/" + project.name to role))
        }
            .also { logger.debug("Got ${it.size} users: $it") }
    }
}
