package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.LnkUserProjectRepository
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.LnkUserProject
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.User
import org.springframework.stereotype.Service

/**
 * Service of lnkUserProjects
 */
@Service
class LnkUserProjectService(private val lnkUserProjectRepository: LnkUserProjectRepository) {
    /**
     * @param project
     * @return all users with role in project
     */
    fun getAllUsersAndRolesByProject(project: Project) =
            lnkUserProjectRepository.findByProject(project).associate { it.user to (it.role ?: Role.NONE) }

    /**
     * @param userId
     * @param project
     * @return role for user in [project] by user ID
     */
    fun findRoleByUserIdAndProject(userId: Long, project: Project) = lnkUserProjectRepository
        .findByUserIdAndProject(userId, project)
        ?.role
        ?: Role.NONE

    /**
     * Set role of [user] on a project [project] to [role]
     *
     * @throws IllegalStateException if [role] is [Role.NONE]
     */
    @Suppress("KDOC_WITHOUT_PARAM_TAG", "UnsafeCallOnNullableType")
    fun setRole(user: User, project: Project, role: Role) {
        if (role == Role.NONE) {
            throw IllegalStateException("Role NONE should not be present in database!")
        }
        val lnkUserProject = lnkUserProjectRepository.findByUserIdAndProject(user.id!!, project)
            ?.apply { this.role = role }
            ?: LnkUserProject(project, user, role)
        lnkUserProjectRepository.save(lnkUserProject)
    }
}
