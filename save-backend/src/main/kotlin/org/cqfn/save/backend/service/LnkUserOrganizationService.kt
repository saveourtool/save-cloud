package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.LnkUserOrganizationRepository
import org.cqfn.save.domain.Role
import org.springframework.stereotype.Service

/**
 * Service of lnkUserOrganizationRepository
 */
@Service
class LnkUserOrganizationService(
    private val lnkUserOrganizationRepository: LnkUserOrganizationRepository,
) {
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
