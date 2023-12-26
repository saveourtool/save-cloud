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
     * @param organizationId organization id for update
     * @param rating new organization rating
     * @return updated organization
     */
    @Query(
        value = "update save_cloud.organization o set o.rating = :rating where o.id = :user_id",
        nativeQuery = true,
    )
    fun updateOrganization(
        @Param("organization_id") organizationId: Long,
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
