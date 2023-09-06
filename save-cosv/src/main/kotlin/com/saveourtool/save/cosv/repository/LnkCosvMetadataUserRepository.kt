package com.saveourtool.save.cosv.repository

import com.saveourtool.save.entities.cosv.LnkCosvMetadataUser
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository of LnkCosvMetadataUser
 */
@Repository
interface LnkCosvMetadataUserRepository : BaseEntityRepository<LnkCosvMetadataUser> {
    /**
     * @param cosvMetadataId id of cosv metadata
     * @return list of LnkCosvMetadataUser link to cosv metadata
     */
    fun findByCosvMetadataId(cosvMetadataId: Long): List<LnkCosvMetadataUser>

    /**
     * @param userName name of user
     * @param cosvMetadataId id of cosv metadata
     */
    fun deleteByUserNameAndCosvMetadataId(userName: String, cosvMetadataId: Long)
}
