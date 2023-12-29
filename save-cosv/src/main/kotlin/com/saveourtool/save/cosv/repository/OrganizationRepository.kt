package com.saveourtool.save.cosv.repository

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.LnkUserOrganization
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

    /**
     * @param userId
     * @param organizationId
     * @return found [Role]
     */
    @Query(
        value = "select role from save_cloud.lnk_user_organization where user_id = :user_id and organization_id = :organization_id",
        nativeQuery = true,
    )
    fun findRoleByUserIdAndOrganization(
        @Param("user_id") userId: Long,
        @Param("organization_id") organizationId: Long
    ): Role

    /**
     * @param userId name of user
     * @param organizationStatus status of organization
     * @param organizationName name of organization
     * @return found [LnkUserOrganization]
     */
    @Query(
        value = """select * from save_cloud.lnk_user_organization lnk
            join save_cloud.organization o
            on o.id = lnk.organization_id
            where 1 = 1 
            and user_id = :user_id 
            and o.name = :organization_name
            and o.status = :organization_status""",
        nativeQuery = true,
    )
    fun findByUserNameAndOrganizationStatusAndOrganizationName(
        @Param("user_id") userId: Long,
        @Param("organization_status") organizationStatus: String,
        @Param("organization_name") organizationName: String
    ): LnkUserOrganization?

    /**
     * @param userId
     * @param organizationName
     * @return lnkUserOrganization by user ID and organization
     */
    @Query(
        value = """select role from save_cloud.lnk_user_organization lnk
            join save_cloud.organization o
            on o.id = lnk.organization_id
            where 1 = 1 
            and user_id = :user_id and o.name = :organization_name""",
        nativeQuery = true,
    )
    fun findByUserIdAndOrganizationName(
        @Param("user_id") userId: Long,
        @Param("organization_name") organizationName: String,
    ): LnkUserOrganization?
}
