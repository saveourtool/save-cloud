package com.saveourtool.save.backend.controllers.internal

import com.saveourtool.save.authservice.utils.IdentitySourceAwareUserDetails
import com.saveourtool.save.backend.repository.OriginalLoginRepository
import com.saveourtool.save.backend.service.UserService
import com.saveourtool.save.entities.User
import com.saveourtool.save.utils.IdentitySourceAwareUserDetailsMixin
import com.saveourtool.save.utils.StringResponse

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.jackson2.CoreJackson2Module
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Controller that handles operation with users
 */
@RestController
@RequestMapping("/internal/users")
class UsersController(
    private val userService: UserService,
    private val originalLoginRepository: OriginalLoginRepository,
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
    @Transactional
    fun saveNewUser(@RequestBody user: User) {
        val userName = requireNotNull(user.name) { "Provided user $user doesn't have a name" }

        val userFind = originalLoginRepository.findByNameAndSource(userName, user.source)

        userFind?.user?.let {
            logger.debug("User $userName is already present in the DB")
        } ?: run {
            logger.info("Saving user $userName to the DB")
            userService.saveNewUser(user)
        }
    }

    /**
     * Find user by name
     *
     * @param userName user name
     */
    @GetMapping("/find-by-name/{userName}")
    fun findByUsernameAndSource(
        @PathVariable userName: String,
    ): Mono<StringResponse> = userService.findByName(userName).map {
        ResponseEntity.ok().body(objectMapper.writeValueAsString(it))
    }

    /**
     * Find user by name and source
     *
     * @param source user source
     * @param nameInSource user name
     */
    @GetMapping("/find-by-original-login/{source}/{nameInSource}")
    fun findByUsernameAndSource(
        @PathVariable source: String,
        @PathVariable nameInSource: String,
    ): Mono<StringResponse> = userService.findByOriginalLogin(nameInSource, source).map {
        ResponseEntity.ok().body(objectMapper.writeValueAsString(it))
    }
}
