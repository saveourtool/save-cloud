package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.OrganizationStatus
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
     * @return organization by name
     */
    fun findByName(name: String): Organization?

    /**
     * @param id
     * @return organization by id
     */
    // The getById method from JpaRepository can lead to LazyInitializationException
    fun getOrganizationById(id: Long): Organization

    /**
     * @param ownerId
     * @return list of organization by owner id
     */
    fun findByOwnerId(ownerId: Long): List<Organization>

    /**
     * @param prefix prefix of organization name
     * @param status
     * @return list of organizations with names that start with [prefix]
     */
    fun findByNameStartingWithAndStatus(prefix: String, status: OrganizationStatus): List<Organization>

    fun findByNameStartingWithAndStatusIn(prefix: String, status: List<OrganizationStatus>): List<Organization>

    /**
     * @param status
     * @return list of organizations with required status
     */
    fun findByStatus(status: OrganizationStatus): List<Organization>

    /**
     * @param status
     * @return list of organizations with required status
     */
    fun findByStatusIn(status: List<OrganizationStatus>): List<Organization>
}
