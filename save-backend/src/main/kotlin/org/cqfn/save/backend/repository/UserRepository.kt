package org.cqfn.save.backend.repository

import org.cqfn.save.entities.User
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : BaseEntityRepository<User> {
    fun findByName(username: String) : User?
}
