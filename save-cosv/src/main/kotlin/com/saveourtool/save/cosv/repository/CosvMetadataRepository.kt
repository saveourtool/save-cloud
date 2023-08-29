package com.saveourtool.save.cosv.repository

import com.saveourtool.save.entities.cosv.CosvMetadata
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * JPA repository for [CosvMetadata]
 */
@Repository
interface CosvMetadataRepository : BaseEntityRepository<CosvMetadata>
