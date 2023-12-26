package com.saveourtool.save.cosv.service

import com.saveourtool.save.cosv.repository.UserRepository
import com.saveourtool.save.entities.User
import org.springframework.stereotype.Service

/**
 * Service for user
 */
@Service
class UserService(
    private val userRepository: UserRepository,
) {
    /**
     * @param user user for update
     * @return updated user
     */
    fun saveUser(user: User) = userRepository.updateUser(user.name, user.rating)

    /**
     * @param name
     * @return user with [name]
     */
    fun getUserByName(name: String): User = userRepository.getUserByName(name)
}
