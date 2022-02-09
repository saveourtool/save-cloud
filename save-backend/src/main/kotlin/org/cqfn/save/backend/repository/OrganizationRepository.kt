package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Organization
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.query.QueryByExampleExecutor
import org.springframework.stereotype.Repository

/**
 * The repository of organization entities
 */
@Repository
interface OrganizationRepository : JpaRepository<Organization, Long>, QueryByExampleExecutor<Organization>,
JpaSpecificationExecutor<Organization> {
    /**
     * @param name
     * @return organization by name
     */
    fun findByName(name: String): Organization
}
