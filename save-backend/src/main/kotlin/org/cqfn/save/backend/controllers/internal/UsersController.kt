package org.cqfn.save.backend.controllers.internal

import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.entities.User
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller that handles operation with users
 */
@RestController
@RequestMapping("/internal/users")
class UsersController(
    private val userRepository: UserRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Stores [user] in the DB
     *
     * @param user user to store
     */
    @PostMapping("/new")
    fun saveNewUser(@RequestBody user: User) {
        val userName = requireNotNull(user.name) { "Provided user $user doesn't have a name" }
        userRepository.findByName(userName).ifPresentOrElse({
            logger.debug("User $userName is already present in the DB")
        }) {
            logger.info("Saving user $userName to the DB")
            userRepository.save(user)
        }
    }
}
