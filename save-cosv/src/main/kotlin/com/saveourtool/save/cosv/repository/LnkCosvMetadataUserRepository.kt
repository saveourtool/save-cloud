package com.saveourtool.save.cosv.repository

import com.saveourtool.save.entities.vulnerabilities.LnkVulnerabilityUser
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository of lnkVulnerabilityUserRepository
 */
@Repository
interface LnkCosvMetadataUserRepository : BaseEntityRepository<LnkVulnerabilityUser> {
    /**
     * @param id id of vulnerability
     * @return list of LnkVulnerabilityUser link to vulnerability
     */
    fun findByVulnerabilityId(id: Long): List<LnkVulnerabilityUser>

    /**
     * @param userName name of user
     * @param vulnerabilityId id of vulnerability
     */
    fun deleteByUserNameAndVulnerabilityId(userName: String, vulnerabilityId: Long)
}
