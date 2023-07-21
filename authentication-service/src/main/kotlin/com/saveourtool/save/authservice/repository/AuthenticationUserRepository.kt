package com.saveourtool.save.authservice.repository

import com.saveourtool.save.entities.User
import com.saveourtool.save.info.UserStatus
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

/**
 * Repository for [com.saveourtool.save.entities.User]
 * @property namedParameterJdbcTemplate
 */
@Component
class AuthenticationUserRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    /**
     * @param name name of user
     * @return user or null if no results have been found
     */
    fun findByName(name: String): User? = namedParameterJdbcTemplate.queryForList(
        "SELECT * FROM save_cloud.user WHERE name = :name",
        mapOf("name" to name)
    ).singleOrNull()?.toUserEntity()

    /**
     * @return Entity [User] created from provided [Map]
     */
    private fun Map<String, Any>.toUserEntity(): User {
        val record = this
        return User(
            name = record["name"] as String,
            password = record["password"] as String?,
            role = record["role"] as String?,
            email = record["email"] as String?,
            avatar = record["avatar"] as String?,
            company = record["company"] as String?,
            location = record["location"] as String?,
            linkedin = record["linkedin"] as String?,
            gitHub = record["git_hub"] as String?,
            twitter = record["twitter"] as String?,
            status = record["status"] as UserStatus,
            rating = record["rating"] as Long,
        ).apply {
            this.id = record["id"] as Long
        }
    }
}
