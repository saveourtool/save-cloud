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
     * @param cosvId [CosvMetadata.cosvId]
     * @param status [CosvMetadata.status]
     * @return found [CosvMetadata] or null
     */
    fun findByCosvIdAndStatus(cosvId: String, status: VulnerabilityStatus): CosvMetadata?
}
