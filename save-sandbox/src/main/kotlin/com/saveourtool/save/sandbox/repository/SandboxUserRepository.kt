package com.saveourtool.save.sandbox.repository

import com.saveourtool.save.entities.User
import com.saveourtool.save.utils.orNotFound
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Repository for [com.saveourtool.save.entities.User]
 */
@Component
class SandboxUserRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    /**
     * @param name
     * @return ID of [com.saveourtool.save.entities.User]
     */
    @Transactional(readOnly = true)
    fun getIdByName(name: String): Long =
            namedParameterJdbcTemplate.queryForObject(
                "SELECT id FROM save_cloud.user WHERE name = :name",
                mapOf("name" to name),
                Long::class.java
            )
                .orNotFound { "There is no user with name $name" }

    /**
     * @param username
     * @return user or null if no results have been found
     */
    @Transactional
    fun findByName(name: String): User? =
        namedParameterJdbcTemplate.queryForObject(
            "SELECT id FROM save_cloud.user WHERE name = :name",
            mapOf("name" to name),
            User::class.java
        )
            .orNotFound { "There is no user with name $name" }


    /**
     * @param name
     * @param source
     * @return user or null if no results have been found
     */
    fun findByNameAndSource(name: String, source: String): User? =
        namedParameterJdbcTemplate.queryForObject(
            "SELECT id FROM save_cloud.user WHERE name = :name AND source = :source",
            mapOf("name" to name, "source" to source),
            User::class.java
        )
            .orNotFound { "There is no user with name $name" }
}
