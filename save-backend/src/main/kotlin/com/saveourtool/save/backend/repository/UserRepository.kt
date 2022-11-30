package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.User
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * Repository to access data about users
 */
@Repository
interface UserRepository : BaseEntityRepository<User>, ValidateRepository {
    /**
     * @param username
     * @return user or null if no results have been found
     */
    fun findByName(username: String): User?

    /**
     * @param username
     * @param ids set of id of people that should not be found
     * @return list of users with [username] except those whose ids are in [ids]
     */
    fun findByNameAndIdNotIn(username: String, ids: Set<Long>): List<User>

    /**
     * @param prefix
     * @param ids
     * @param page
     * @return page of users with names that start with [prefix]
     */
    fun findByNameStartingWithAndIdNotIn(prefix: String, ids: Set<Long>, page: Pageable): Page<User>
  
    /**
     * @param name
     * @param source
     * @return user or null if no results have been found
     */
    fun findByNameAndSource(name: String, source: String): User?
}
