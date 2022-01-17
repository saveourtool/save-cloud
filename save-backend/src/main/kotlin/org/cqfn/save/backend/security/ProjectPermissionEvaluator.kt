package org.cqfn.save.backend.security

import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Project
import org.springframework.security.core.Authentication

/**
 * Class that is capable of assessing user's permissions regarding projects.
 */
class ProjectPermissionEvaluator {
    /**
     * @param authentication [Authentication] describing an authenticated request
     * @param project
     * @param permission
     * @return whether user described by [authentication] can have [permission] on project [project]
     */
    fun hasPermission(authentication: Authentication, project: Project, permission: String): Boolean {
        if (authentication.hasRole(Role.ADMIN)) {
            return true
        }

        val userId = (authentication.details as AuthenticationDetails).id
        return when (permission) {
            "read" -> project.public || hasWriteAccess(userId, project)
            "write" -> hasWriteAccess(userId, project)
            "delete" -> project.userId == userId
            else -> false
        }
    }

    private fun Authentication.hasRole(role: Role): Boolean = authorities.any { it.authority == role.asSpringSecurityRole() }

    private fun hasWriteAccess(userId: Long?, project: Project) =
            userId != null && (project.userId == userId || userId in project.adminIdList())
}
