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
        val id  = namedParameterJdbcTemplate.queryForObject(
            "SELECT id FROM save_cloud.user WHERE name = :name",
            mapOf("name" to name),
            Long::class.java
        ).also {
            println("\nFUCK1")
        }
            .orNotFound { "There is no user with name $name" }
        println("\n\n\n---------------ID ${id}")

        val user = namedParameterJdbcTemplate.queryForMap(
            "SELECT * FROM save_cloud.user WHERE name = :name",
            mapOf("name" to name)
        )

        println("USER1: ${user}")


        val a = user.values as User


        println("USER2: ${a}")

//        return user.mapValues { //entry ->
//            //val (name, password, role, source, email, avatar, company, location, linkedin, gitHub, twitter, isActive, originalLogins) = entry.value
//            User(
//                it.value[0]
//            )
//        }

        return null
    }



    /**
     * @param name
     * @param source
     * @return user or null if no results have been found
     */
    fun findByNameAndSource(name: String, source: String): User? =
        namedParameterJdbcTemplate.queryForList(
            "SELECT * FROM save_cloud.user WHERE name = :name AND source = :source",
            mapOf("name" to name, "source" to source),
            User::class.java
        )
            .orNotFound {
                println("There is no user with name $name and source $source")
                "There is no user with name $name and source $source"
            }.single()
}
