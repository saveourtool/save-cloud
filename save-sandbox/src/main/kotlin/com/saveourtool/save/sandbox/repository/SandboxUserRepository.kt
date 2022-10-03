package com.saveourtool.save.sandbox.repository

import com.saveourtool.save.utils.orNotFound
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

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
                .orNotFound { "There is not user with name $name" }

    /**
     * @param id
     * @return name of [com.saveourtool.save.entities.User]
     */
    @Transactional(readOnly = true)
    fun getNameById(id: Long): String =
            namedParameterJdbcTemplate.queryForObject(
                "SELECT name FROM save_cloud.user WHERE id = :id",
                mapOf("id" to id),
                String::class.java
            )
                .orNotFound { "There is not user with id $id" }
}
