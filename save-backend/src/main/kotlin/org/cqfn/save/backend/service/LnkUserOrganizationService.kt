package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.LnkUserOrganizationRepository
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.*
import org.springframework.stereotype.Service

/**
 * Service of lnkUserOrganization
 */
@Service
class LnkUserOrganizationService(private val lnkUserOrganizationRepository: LnkUserOrganizationRepository) {
    /**
     * @param organization
     * @return all users with role in [organization]
     */
    fun getAllUsersAndRolesByOrganization(organization: Organization) =
            lnkUserOrganizationRepository.findByOrganization(organization).associate { it.user to (it.role ?: Role.NONE) }

    /**
     * @param userId
     * @param organization
     * @return role for user in [organization] by []
     */
    fun findRoleByUserIdAndOrganization(userId: Long, organization: Organization) = lnkUserOrganizationRepository
        .findByUserIdAndOrganization(userId, organization)
        ?.role
        ?: Role.NONE

    /**
     * Set [role] of [user] in [organization]
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
}
