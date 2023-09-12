package com.saveourtool.save.cosv.repository

import com.saveourtool.save.entities.cosv.CosvMetadataProject
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository of vulnerabilityProject
 */
@Repository
interface CosvMetadataProjectRepository : BaseEntityRepository<CosvMetadataProject> {
    /**
     * @param identifier vulnerability identifier
     * @return list of vulnerabilityProjects link to vulnerability
     */
    fun findByCosvMetadataCosvId(identifier: String): List<CosvMetadataProject>

    /**
     * @param name name of project
     * @param identifier vulnerability identifier
     */
    fun deleteByNameAndCosvMetadataCosvId(name: String, identifier: String)
}
