package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.BannedOrganization
import com.saveourtool.save.entities.BannedProject
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
interface BanProjectRepository : JpaRepository<BannedProject, Long>,
QueryByExampleExecutor<BannedProject>,
JpaSpecificationExecutor<BannedProject>,
ValidateRepository {
    /**
     * @param name
     */
    @Query("""
        |insert into save_cloud.high_level_names 
        |set name = :name 
        |set private_comment = :privateComment
        |set public_comment = :publicComment
        |""",
        nativeQuery = true)
    @Modifying
    fun saveBannedProject(@Param("name") name: String, @Param("private_comment") privateComment: String, @Param("public_comment") publicComment: String, )

    /**
     * @param name
     */
    fun deleteBannedProjectByName(name: String)

    /**
     * find by name
     */
    fun findBannedProjectByName(name: String): BannedProject
}
