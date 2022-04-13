package org.cqfn.save.backend.controllers.internal

import com.fasterxml.jackson.databind.ObjectMapper
import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.backend.service.UserDetailsService
import org.cqfn.save.entities.User
import org.cqfn.save.utils.IdentitySourceAwareUserDetails
import org.cqfn.save.utils.IdentitySourceAwareUserDetailsMixin
import org.cqfn.save.utils.extractUserNameAndSource
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.jackson2.CoreJackson2Module
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

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
     * @param userInformation
     */
    @GetMapping("/{userInformation}")
    fun findByUsernameAndSource(
        @PathVariable userInformation: String,
    ): Mono<ResponseEntity<String>> {
        val (name, source) = extractUserNameAndSource(userInformation)
        println("\nfindByUsername: ${userInformation} $name $source")
        return userService.findByUsernameAndSource(name, source).map {
            ResponseEntity.ok().body(objectMapper.writeValueAsString(it))
        }
    }
}
