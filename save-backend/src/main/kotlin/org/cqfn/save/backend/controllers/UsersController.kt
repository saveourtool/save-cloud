package org.cqfn.save.backend.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.entities.User
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.jackson2.CoreJackson2Module
import org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/users")
class UsersController(
    private val userRepository: UserRepository,
) {
    private val objectMapper = ObjectMapper()
        .findAndRegisterModules()
        .registerModule(CoreJackson2Module())
        .registerModule(OAuth2ClientJackson2Module())

    private val logger = LoggerFactory.getLogger(javaClass)

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
