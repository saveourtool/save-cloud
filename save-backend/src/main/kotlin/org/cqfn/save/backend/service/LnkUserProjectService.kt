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
     * @param role
     * @return all users with their roles in project
     */
    fun getAllUsersByProjectAndRole(project: Project, role: Role) = lnkUserProjectRepository.findByProject(project)
        .filter { it.role == role }
        .map { it.user }

    /**
     * @param project
     * @return all users with role in project
     */
    fun getAllUsersAndRolesByProject(project: Project) =
            lnkUserProjectRepository.findByProject(project).associate { it.user to it.role }

    /**
     * @param userId
     * @param project
     * @return role for user in [project] by user ID
     */
    fun findRoleByUserIdAndProject(userId: Long, project: Project) = lnkUserProjectRepository
        .findByUserIdAndProject(userId, project)
        ?.role
        ?: throw IllegalStateException("Multiple roles are set for userId=$userId and project=$project")

    /**
     * Set role of [user] on a project [project] to [role]
     */
    @Suppress("KDOC_WITHOUT_PARAM_TAG")
    fun setRole(user: User, project: Project, role: Role) {
        val lnkUserProject = lnkUserProjectRepository.findByUserIdAndProject(user.id!!, project)?.apply { this.role = role }
            ?: LnkUserProject(project, user, role)
        lnkUserProjectRepository.save(lnkUserProject)
    }
}
