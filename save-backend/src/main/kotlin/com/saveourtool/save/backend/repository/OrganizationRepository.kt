package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.Organization
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
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

    /**
     * @param organizationName
     * @return 1 if [organizationName] is valid, 0 otherwise
     */
    @Query("""select if (count(*) = 0, true, false) from save_cloud.high_level_names where name = :org_name;""", nativeQuery = true)
    fun validateOrganizationName(@Param("org_name")organizationName: String): Long

    /**
     * @param organizationName
     */
    @Query("""insert into save_cloud.high_level_names values (:org_name);""", nativeQuery = true)
    @Modifying
    fun saveOrganizationName(@Param("org_name")organizationName: String)
}
