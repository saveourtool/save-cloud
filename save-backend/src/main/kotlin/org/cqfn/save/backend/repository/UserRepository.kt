package org.cqfn.save.backend.repository

import org.cqfn.save.entities.User
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * Repository to access data about users
 */
@Repository
interface UserRepository : BaseEntityRepository<User> {
    /**
     * @param username
     * @return user or null if no results have been found
     */
    fun findByName(username: String): Optional<User>

    /**
     * @param name
     * @param source
     * @return user or null if no results have been found
     */
    fun findByNameAndSource(name: String, source: String): Optional<User>
}
