package com.saveourtool.save.authservice.repository

import com.saveourtool.save.authservice.utils.toUserEntity
import com.saveourtool.save.entities.User
import com.saveourtool.save.utils.orNotFound
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

/**
 * Repository for [com.saveourtool.save.entities.User]
 */
@Component
class AuthenticationUserRepository(
    protected val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    /**
     * @param name name of user
     * @return user or null if no results have been found
     */
    fun findByNameAndSource(name: String): User? {
        return namedParameterJdbcTemplate.queryForList(
            "SELECT * FROM save_cloud.user WHERE name = :name",
            mapOf("name" to name)
        ).singleOrNull()?.toUserEntity()
    }

    /**
     * @param name name of user
     * @return user or error if no results have been found
     */
    fun getByNameAndSource(name: String): User = findByNameAndSource(name)
        .orNotFound {
            "There is no user with name $name"
        }
}
