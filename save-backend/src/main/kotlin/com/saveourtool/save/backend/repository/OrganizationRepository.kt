package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.OrganizationStatus
import com.saveourtool.save.repository.ValidateRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.query.QueryByExampleExecutor
import org.springframework.stereotype.Repository

/**
 * The repository of organization entities
 */
@Repository
interface OrganizationRepository : JpaRepository<Organization, Long>,
QueryByExampleExecutor<Organization>,
JpaSpecificationExecutor<Organization>,
ValidateRepository {
    /**
     * @param name
     * @return organization by [name]
     */
    fun findByName(name: String): Organization?

    /**
     * @param name
     * @param statuses
     * @return organization by [name] and [statuses]
     */
    fun findByNameAndStatusIn(name: String, statuses: Set<OrganizationStatus>): Organization?

    /**
     * @param id
     * @return organization by id
     */
    // The getById method from JpaRepository can lead to LazyInitializationException
    fun getOrganizationById(id: Long): Organization

    /**
     * @param prefix prefix of organization name
     * @param statuses is set of statuses, one of which an organization can have
     * @param pageable [Pageable]
     * @return list of organizations with names that start with [prefix]
     */
    fun findByNameStartingWithAndStatusIn(prefix: String, statuses: Set<OrganizationStatus>, pageable: Pageable): List<Organization>
}
