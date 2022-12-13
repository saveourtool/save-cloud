package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.BannedOrganization
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
interface BanOrganizationRepository : JpaRepository<BannedOrganization, Long>,
QueryByExampleExecutor<BannedOrganization>,
JpaSpecificationExecutor<BannedOrganization>,
ValidateRepository {
    /**
     * @param name
     * @param publicComment
     * @param privateComment
     */
    @Query("""
        |insert into save_cloud.high_level_names 
        |set name = :name 
        |set private_comment = :privateComment
        |set public_comment = :publicComment
        |""",
        nativeQuery = true)
    @Modifying
    fun saveBannedOrganization(@Param("name") name: String, @Param("private_comment") privateComment: String, @Param("public_comment") publicComment: String, )

    /**
     * @param name
     */
    fun deleteBannedOrganizationByName(name: String)

    /**
     * find by name
     */
    fun findBannedOrganizationByName(name: String): BannedOrganization
}
