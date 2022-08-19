package com.saveourtool.save.backend.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ValidateRepository {

    /**
     * @param name of organization or user
     * @return 1 if [name] is valid, 0 otherwise
     */
    @Query("""select if (count(*) = 0, true, false) from save_cloud.high_level_names where name = :name""", nativeQuery = true)
    fun validateName(@Param("name") name: String): Long

    /**
     * @param name
     */
    @Query("""insert into save_cloud.high_level_names set name = :name""", nativeQuery = true)
    @Modifying
    fun saveHighName(@Param("name") name: String)

}
