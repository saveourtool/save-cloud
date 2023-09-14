package com.saveourtool.save.cosv.repository

import com.saveourtool.save.entities.cosv.CosvFile
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Repository for [CosvFile]
 */
@Repository
interface CosvObjectRepository : BaseEntityRepository<CosvFile> {
    /**
     * @param identifier
     * @param modifier
     * @return [CosvFile] found by provided values
     */
    fun findByIdentifierAndModified(identifier: String, modifier: LocalDateTime): CosvFile?
}
