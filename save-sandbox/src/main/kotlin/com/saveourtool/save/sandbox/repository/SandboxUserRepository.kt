package com.saveourtool.save.sandbox.repository

import com.fasterxml.jackson.annotation.JsonIgnore
import com.saveourtool.save.entities.OriginalLogin
import com.saveourtool.save.entities.User
import com.saveourtool.save.utils.orNotFound
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import javax.persistence.FetchType
import javax.persistence.OneToMany

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
     * @param name
     * @return user or null if no results have been found
     */
    @Transactional
    fun findByName(name: String): User? {
        println("findByName namedParameterJdbcTemplate...")

        val record = namedParameterJdbcTemplate.queryForList(
            "SELECT * FROM save_cloud.user WHERE name = :name",
            mapOf("name" to name)
        ).single()
            .orNotFound {
                "There is no user with name $name"
            }

        return record.toUserEntity()
    }



    /**
     * @param name
     * @param source
     * @return user or null if no results have been found
     */
    fun findByNameAndSource(name: String, source: String): User? {
        val record = namedParameterJdbcTemplate.queryForList(
            "SELECT * FROM save_cloud.user WHERE name = :name AND source = :source",
            mapOf("name" to name, "source" to source),
        ).single()
            .orNotFound {
                "There is no user with name $name and source $source"
            }
        return record.toUserEntity()
    }


    private fun Map<String, Any>.toUserEntity(): User {
        val record = this
        return User(
            name = record["name"] as String?,
            password = record["password"] as String?,
            role = record["role"] as String?,
            source = record["source"] as String,
            email = record["email"] as String?,
            avatar = record["avatar"] as String?,
            company = record["company"] as String?,
            location = record["location"] as String?,
            linkedin = record["linkedin"] as String?,
            gitHub = record["git_hub"] as String?,
            twitter = record["twitter"] as String?,
            isActive = record["is_active"] as Boolean,
        ).apply {
            this.id = record["id"] as Long
        }
    }
}
