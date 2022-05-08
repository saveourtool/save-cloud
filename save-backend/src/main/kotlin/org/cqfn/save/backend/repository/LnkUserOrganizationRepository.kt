package org.cqfn.save.backend.repository

import org.cqfn.save.entities.LnkUserOrganization
import org.cqfn.save.entities.Organization
import org.springframework.stereotype.Repository

/**
 * Repository of lnkUserProject
 */
@Repository
interface LnkUserOrganizationRepository : BaseEntityRepository<LnkUserOrganization> {
    /**
     * @param organization
     * @return lnkUserOrganization by organization
     */
    fun findByOrganization(organization: Organization): List<LnkUserOrganization>

    /**
     * @param userId
     * @param organizationName
     * @return lnkUserOrganization by user ID and organization
     */
    fun findByUserIdAndOrganizationName(userId: Long, organizationName: String): LnkUserOrganization?

    /**
     * @param userId
     * @param organization
     * @return [LnkUserOrganization] if user is connected to [organization] and `null` otherwise
     */
    fun findByUserIdAndOrganization(userId: Long, organization: Organization): LnkUserOrganization?
}
