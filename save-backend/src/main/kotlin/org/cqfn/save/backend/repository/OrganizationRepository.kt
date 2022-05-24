package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.Organization
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
}
