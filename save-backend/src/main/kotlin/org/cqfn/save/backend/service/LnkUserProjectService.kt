package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.LnkUserProjectRepository
import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.LnkUserProject
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.User
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service of lnkUserProjects
 */
@Service
class LnkUserProjectService(
    private val lnkUserProjectRepository: LnkUserProjectRepository,
    private val userRepository: UserRepository
) {
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

    /**
     * @param user that should be deleted from [project]
     * @param project
     * @return none
     */
    fun removeRole(user: User, project: Project) = lnkUserProjectRepository.findByUserIdAndProject(user.id!!, project)
        ?.id
        ?.let {
            lnkUserProjectRepository.deleteById(it)
        }
        ?: run {
            logger.warn("Cannot delete user ${user.name ?: user.id!!} from project ${project.organization.name}/${project.name}: no such link was found.")
        }

    /**
     * Get all platform users
     *
     * @return list of all save-cloud users
     */
    fun getAllUsers(): List<User> = userRepository.findAll()

    /**
     * @param project
     * @return list of [User]s that are connected to [project]
     */
    fun getAllUsersByProject(project: Project): List<User> = lnkUserProjectRepository.findByProject(project).map { it.user }
    companion object {
        private val logger = LoggerFactory.getLogger(LnkUserProject::class.java)
    }
}
