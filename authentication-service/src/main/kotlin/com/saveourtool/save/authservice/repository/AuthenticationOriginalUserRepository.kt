package com.saveourtool.save.authservice.repository

import com.saveourtool.save.authservice.utils.toUserEntity
import com.saveourtool.save.entities.User
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

/**
 * Repository for [com.saveourtool.save.entities.User]
 */
@Component
class AuthenticationOriginalUserRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    /**
     * @param name name of user
     * @param source source of user
     * @return user or null if no results have been found
     */
    fun findByNameAndSource(name: String, source: String): User? =
        namedParameterJdbcTemplate.queryForList(
            "SELECT * FROM save_cloud.user WHERE id = (select user_id from save_cloud.original_login where name = :name AND source = :source)",
            mapOf("name" to name, "source" to source)
        ).singleOrNull()?.toUserEntity()
}
