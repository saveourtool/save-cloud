package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.LnkUserOrganizationRepository
import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.*
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

/**
 * Service of lnkUserOrganization
 */
@Service
class LnkUserOrganizationService(
    private val lnkUserOrganizationRepository: LnkUserOrganizationRepository,
    private val userRepository: UserRepository,
) {
    /**
     * @param organization
     * @return all users with role in [organization]
     */
    fun getAllUsersAndRolesByOrganization(organization: Organization) =
            lnkUserOrganizationRepository.findByOrganization(organization)
                .associate { it.user to (it.role ?: Role.NONE) }

    /**
     * @param userId
     * @param organization
     * @return role for user in [organization]
     */
    fun findRoleByUserIdAndOrganization(userId: Long, organization: Organization) = lnkUserOrganizationRepository
        .findByUserIdAndOrganization(userId, organization)
        ?.role
        ?: Role.NONE

    /**
     * Set [role] of [user] in [organization].
     *
     * @throws IllegalStateException if [role] is [Role.NONE]
     */
    @Suppress("KDOC_WITHOUT_PARAM_TAG", "UnsafeCallOnNullableType")
    fun setRole(user: User, organization: Organization, role: Role) {
        if (role == Role.NONE) {
            throw IllegalStateException("Role NONE should not be present in database!")
        }
        val lnkUserOrganization = lnkUserOrganizationRepository.findByUserIdAndOrganization(user.id!!, organization)
            ?.apply { this.role = role }
            ?: LnkUserOrganization(organization, user, role)
        lnkUserOrganizationRepository.save(lnkUserOrganization)
    }

    /**
     * @param user
     * @param organization
     * @return role of the [user] in [organization]
     */
    @Suppress("KDOC_WITHOUT_PARAM_TAG", "UnsafeCallOnNullableType")
    fun getRole(user: User, organization: Organization) = lnkUserOrganizationRepository
        .findByUserIdAndOrganization(user.id!!, organization)
        ?.role
        ?: Role.NONE

    /**
     * @param userName
     * @return user with [userName]
     */
    fun getUserByName(userName: String) = userRepository.findByName(userName)

    /**
     * @param userId
     * @return user with [userId]
     */
    fun getUserById(userId: Long) = userRepository.findById(userId)

    /**
     * Removes role of [user] in [organization].
     *
     * @param user
     * @param organization
     * @return Unit
     */
    @Suppress("KDOC_WITHOUT_PARAM_TAG", "UnsafeCallOnNullableType")
    fun removeRole(user: User, organization: Organization) = lnkUserOrganizationRepository
        .findByUserIdAndOrganization(user.id!!, organization)
        ?.id
        ?.let { lnkUserOrganizationRepository.deleteById(it) }
        ?: throw NoSuchElementException(
            "Cannot delete user with name ${user.name} because he is not found in organization ${organization.name}"
        )

    /**
     * @param name
     * @param organizationUserIds
     * @return list of all users who have [name] and whose ids are not in [organizationUserIds]
     */
    fun getNonOrganizationUsersByName(name: String, organizationUserIds: Set<Long>) = userRepository.findByNameAndIdNotIn(name, organizationUserIds)

    /**
     * @param prefix
     * @param organizationUserIds
     * @param pageSize
     * @return list of [pageSize] users whose name starts with [prefix] and whose ids are not in [organizationUserIds]
     */
    fun getNonOrganizationUsersByNamePrefix(prefix: String, organizationUserIds: Set<Long>, pageSize: Int) = if (pageSize > 0) {
        userRepository.findByNameStartingWithAndIdNotIn(prefix, organizationUserIds, PageRequest.of(0, pageSize)).content
    } else {
        emptyList()
    }

    /**
     * @param userId
     * @param organizationName
     * @return role for user in organization by user ID and organization name
     */
    fun findRoleByUserIdAndOrganizationName(userId: Long, organizationName: String) = lnkUserOrganizationRepository
        .findByUserIdAndOrganizationName(userId, organizationName)
        ?.role
        ?: Role.NONE
}
