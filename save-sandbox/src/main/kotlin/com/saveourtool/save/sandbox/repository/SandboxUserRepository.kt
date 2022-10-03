package com.saveourtool.save.sandbox.repository

import com.saveourtool.save.utils.orNotFound
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SandboxUserRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    /**
     * @param name
     * @return ID of [com.saveourtool.save.entities.User]
     */
    @Transactional(readOnly = true)
    fun getIdByName(name: String): Long =
            jdbcTemplate.queryForObject("SELECT id FROM save_cloud.user WHERE name = $name", Long::class.java)
                .orNotFound { "There is not user with name $name" }

    /**
     * @param id
     * @return name of [com.saveourtool.save.entities.User]
     */
    @Transactional(readOnly = true)
    fun getNameById(id: Long): String =
            jdbcTemplate.queryForObject("SELECT name FROM save_cloud.user WHERE id = $id", String::class.java)
            .orNotFound { "There is not user with id $id" }
}
