package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.OriginalLogin
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Repository to access data about original user logins and sources
 */
@Repository
interface OriginalLoginRepository : BaseEntityRepository<OriginalLogin> {
    /**
     * @param username
     * @return OriginalLogin or null if no results have been found
     */
    fun findByName(username: String): OriginalLogin?

    /**
     * @param name
     * @param source
     * @return user or null if no results have been found
     */
    fun findByNameAndSource(name: String, source: String): OriginalLogin?

    /**
     * @param id id of user
     */
    fun deleteByUserId(id: Long)
}
