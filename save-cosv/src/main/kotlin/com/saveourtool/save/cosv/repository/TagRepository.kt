package com.saveourtool.save.cosv.repository

import com.saveourtool.save.entities.Tag
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * The repository of tag entities.
 */
@Repository
interface TagRepository {
    /**
     * Find [Tag] by its [Tag.name]
     *
     * @param name tag name
     * @return [Tag] if found, null otherwise
     */
    @Query(
        value = "select * from save_cloud.tag t where t.name = :name",
        nativeQuery = true,
    )
    fun findByName(@Param("name") name: String): Tag?

    /**
     * @param name name of tag
     * @return save tag
     */
    @Transactional
    @Modifying
    @Query(
        value = "insert into save_cloud.tag (name) values (:name)",
        nativeQuery = true,
    )
    fun saveTag(
        @Param("name") name: String,
    )
}
