package com.saveourtool.save.cosv.repository

import com.saveourtool.save.entities.Organization
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * The repository of organization entities
 */
@Repository
interface OrganizationRepository {
    /**
     * @param organizationName organization name for update
     * @param rating new organization rating
     * @return updated organization
     */
    @Query(
        value = "update save_cloud.organization o set o.rating = :rating where o.name = :organization_name",
        nativeQuery = true,
    )
    fun updateOrganization(
        @Param("organization_name") organizationName: String,
        @Param("rating") rating: Long,
    )

    /**
     * @param name name of organization
     * @return found [Organization] by name
     */
    @Query(
        value = "select * from save_cloud.organization where name = :name",
        nativeQuery = true,
    )
    fun getOrganizationByName(@Param("name") name: String): Organization
}
