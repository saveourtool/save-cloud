package org.cqfn.save.backend.security

import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Project
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.core.Authentication
import java.io.Serializable

class ProjectPermissionEvaluator {
    fun hasPermission(authentication: Authentication, project: Project, permission: String): Boolean {
        if (authentication.hasRole(Role.ADMIN)) return true

        val userId = (authentication.details as AuthenticationDetails).id
        return when (permission) {
            "read" -> project.public || hasWriteAccess(userId, project)
            "write" -> hasWriteAccess(userId, project)
            "delete" -> project.userId == userId
            else -> false
        }
    }

    private fun Authentication.hasRole(role: Role): Boolean {
        return authorities.any { it.authority == role.asSpringSecurityRole() }
    }

    private fun hasWriteAccess(userId: Long?, project: Project) =
        userId != null && (project.userId == userId || userId in project.adminIdList())
}