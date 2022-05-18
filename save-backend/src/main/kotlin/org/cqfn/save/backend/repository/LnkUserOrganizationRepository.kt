package org.cqfn.save.backend.repository

import org.cqfn.save.entities.LnkUserOrganization
import org.cqfn.save.entities.Organization
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

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

    /**
     * @param userId
     * @param organizationId
     * @return [LnkUserOrganization] if user is connected to organization with [organizationId] and `null` otherwise
     */
    fun findByUserIdAndOrganizationId(userId: Long, organizationId: Long): LnkUserOrganization?

    /**
     * Save [LnkUserOrganization] using only ids and role string.
     *
     * @param userId
     * @param organizationId
     * @param role
     */
    @Transactional
    @Modifying
    @Query(
        value = "insert into save_cloud.lnk_user_organization (organization_id, user_id, role) values (:organization_id, :user_id, :role)",
        nativeQuery = true,
    )
    fun save(
        @Param("organization_id") organizationId: Long,
        @Param("user_id") userId: Long,
        @Param("role") role: String
    )

    /**
     * @param userId
     * @return List of [LnkUserOrganization] in which user with [userId] participates
     */
    fun findByUserId(userId: Long): List<LnkUserOrganization>
}
