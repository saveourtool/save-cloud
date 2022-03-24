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

}