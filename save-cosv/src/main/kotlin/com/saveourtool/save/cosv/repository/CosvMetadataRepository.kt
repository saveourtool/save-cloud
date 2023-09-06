package com.saveourtool.save.cosv.repository

import com.saveourtool.save.entities.cosv.CosvMetadata
import com.saveourtool.save.entities.vulnerability.VulnerabilityStatus
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * JPA repository for [CosvMetadata]
 */
@Repository
interface CosvMetadataRepository : BaseEntityRepository<CosvMetadata> {
    /**
     * @param cosvId [CosvMetadata.cosvId]
     * @return found [CosvMetadata] or null
     */
    fun findByCosvId(cosvId: String): CosvMetadata?

    /**
     * @param userId creator of vulnerability
     * @return list of metadata
     */
    fun findByUserId(userId: Long): List<CosvMetadata>

    /**
     * @param userId id of user
     * @param status status of vulnerability
     * @return count of vulnerabilities
     */
    fun countByUserIdAndStatus(userId: Long, status: VulnerabilityStatus): Int

    /**
     * @param organizationName name of organization
     * @param status status of vulnerability
     * @return list of metadata
     */
    fun findByOrganizationNameAndStatus(organizationName: String, status: VulnerabilityStatus): List<CosvMetadata>
}
