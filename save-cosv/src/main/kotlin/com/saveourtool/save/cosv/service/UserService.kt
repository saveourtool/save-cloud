package com.saveourtool.save.cosv.service

import com.saveourtool.save.authservice.utils.userId
import com.saveourtool.save.authservice.utils.username
import com.saveourtool.save.cosv.repositorysave.OrganizationRepository
import com.saveourtool.save.cosv.repositorysave.UserRepository
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.User
import com.saveourtool.save.utils.getHighestRole
import com.saveourtool.save.utils.orNotFound
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service

/**
 * Service for user
 */
@Service
class UserService(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
) {
    /**
     * @param user user for update
     * @return updated user
     */
    fun saveUser(user: User) = userRepository.updateUser(user.name, user.rating)

    /**
     * @param name
     * @return user with [name]
     */
    fun getUserByName(name: String): User = userRepository.findByName(name).orNotFound { "Not found user by name $name" }

    /**
     * @param id
     * @return user with [id]
     */
    fun findById(id: Long): User = userRepository.findByIdOrNull(id).orNotFound { "Not found user by id $id" }

    /**
     * @param ids
     * @return users with [ids]
     */
    fun findAllByIdIn(ids: List<Long>): List<User> = userRepository.findAllByIdIn(ids)

    /**
     * @param authentication
     * @return global [Role] of authenticated user
     */
    fun getGlobalRole(authentication: Authentication): Role = getUserByName(authentication.username())
        .role
        ?.let { Role.fromSpringSecurityRole(it) }
        ?: Role.VIEWER

    /**
     * @param userId
     * @param organizationName
     * @return role for user in organization by user ID and organization name
     */
    fun findRoleByUserIdAndOrganizationName(userId: Long, organizationName: String) = organizationRepository
        .findByUserIdAndOrganizationName(userId, organizationName)
        ?.role
        ?: Role.NONE

    /**
     * @param authentication
     * @param organizationName
     * @return the highest of two roles: the one in organization with name [organizationName] and global one.
     */
    fun getGlobalRoleOrOrganizationRole(authentication: Authentication, organizationName: String): Role {
        val selfId = authentication.userId()
        val selfGlobalRole = getGlobalRole(authentication)
        val selfOrganizationRole = findRoleByUserIdAndOrganizationName(selfId, organizationName)
        return getHighestRole(selfOrganizationRole, selfGlobalRole)
    }
}
