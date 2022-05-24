package com.saveourtool.save.backend.controllers.internal

import com.saveourtool.save.backend.repository.UserRepository
import com.saveourtool.save.backend.service.UserDetailsService
import com.saveourtool.save.entities.User
import com.saveourtool.save.utils.IdentitySourceAwareUserDetails
import com.saveourtool.save.utils.IdentitySourceAwareUserDetailsMixin
import com.saveourtool.save.utils.extractUserNameAndSource

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.jackson2.CoreJackson2Module
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

typealias StringResponse = ResponseEntity<String>

/**
 * Controller that handles operation with users
 */
@RestController
@RequestMapping("/internal/users")
class UsersController(
    private val userRepository: UserRepository,
    private val userService: UserDetailsService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper()
        .findAndRegisterModules()
        .registerModule(CoreJackson2Module())
        .addMixIn(IdentitySourceAwareUserDetails::class.java, IdentitySourceAwareUserDetailsMixin::class.java)

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

    /**
     * Find user by name and source
     *
     * @param userInformation user source and name, separated by `@`
     */
    @GetMapping("/{userInformation}")
    fun findByUsernameAndSource(
        @PathVariable userInformation: String,
    ): Mono<StringResponse> {
        val (name, source) = extractUserNameAndSource(userInformation)
        return userService.findByUsernameAndSource(name, source).map {
            ResponseEntity.ok().body(objectMapper.writeValueAsString(it))
        }
    }
}
