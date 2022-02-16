package org.cqfn.save.backend.security

import org.cqfn.save.backend.service.LnkUserProjectService
import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Project
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication

/**
 * Class that is capable of assessing user's permissions regarding projects.
 */
class ProjectPermissionEvaluator {
    @Autowired
    private lateinit var lnkUserProjectService: LnkUserProjectService

    /**
     * @param authentication [Authentication] describing an authenticated request
     * @param project
     * @param permission
     * @return whether user described by [authentication] can have [permission] on project [project]
     */
    fun hasPermission(authentication: Authentication?, project: Project, permission: Permission): Boolean {
        authentication ?: return when (permission) {
            Permission.READ -> project.public
            Permission.WRITE -> false
            Permission.DELETE -> false
        }

        if (authentication.hasRole(Role.ADMIN)) {
            return true
        }

        val userId = (authentication.details as AuthenticationDetails).id
        return when (permission) {
            Permission.READ -> project.public || hasWriteAccess(userId, project)
            Permission.WRITE -> hasWriteAccess(userId, project)
            Permission.DELETE -> project.userId == userId
        }
    }

    private fun Authentication.hasRole(role: Role): Boolean = authorities.any { it.authority == role.asSpringSecurityRole() }

    private fun hasWriteAccess(userId: Long?, project: Project): Boolean = if (userId != null && project.userId == userId) {
        true
    } else {
        val adminIdList = lnkUserProjectService.getAllUsersByProjectAndRole(project, Role.ADMIN).map { it.id }
        userId != null && userId in adminIdList
    }
}
