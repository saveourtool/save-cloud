package com.saveourtool.save.backend.controllers.internal

import com.saveourtool.save.backend.repository.OriginalLoginRepository
import com.saveourtool.save.backend.service.UserDetailsService
import com.saveourtool.save.domain.Role

import com.fasterxml.jackson.databind.ObjectMapper
import com.saveourtool.save.authservice.utils.AuthenticationUserDetails
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.userdetails.User as SpringUser
import org.springframework.security.jackson2.CoreJackson2Module
import org.springframework.security.jackson2.SecurityJackson2Modules
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

typealias AuthenticationUserDetailsResponse = ResponseEntity<AuthenticationUserDetails>

/**
 * Controller that handles operation with users
 */
@RestController
@RequestMapping("/internal/users")
class UsersController(
    private val userService: UserDetailsService,
    private val originalLoginRepository: OriginalLoginRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val springUserDetailsWriter = ObjectMapper()
        .findAndRegisterModules()
        .registerModule(CoreJackson2Module())
        .registerModules(SecurityJackson2Modules.getModules(javaClass.classLoader))
        .writerFor(SpringUser::class.java)

    /**
     * Stores user in the DB with provided [name] with [roleForNewUser] as role.
     * And add a link to [source] for created user
     *
     * @param source user source
     * @param name user name
     */
    @PostMapping("/new/{source}/{name}")
    @Transactional
    fun saveNewUserIfRequired(
        @PathVariable source: String,
        @PathVariable name: String,
    ) {
        val userFind = originalLoginRepository.findByNameAndSource(name, source)

        userFind?.user?.let {
            logger.debug("User $name ($source) is already present in the DB")
        } ?: run {
            logger.info("Saving user $name ($source) with authorities $roleForNewUser to the DB")
            val savedUser = userService.saveNewUser(name, roleForNewUser)
            userService.addSource(savedUser, name, source)
        }
    }

    /**
     * Find user by name
     *
     * @param userName user name
     * @return found Spring's UserDetails as a String
     */
    @GetMapping("/find-by-name/{userName}")
    fun findByName(
        @PathVariable userName: String,
    ): Mono<AuthenticationUserDetailsResponse> = userService.findByName(userName).map {
        ResponseEntity.ok().body(AuthenticationUserDetails(it))
    }

    /**
     * Find user by name and source
     *
     * @param source user source
     * @param nameInSource user name
     * @return found Spring's UserDetails as a String
     */
    @GetMapping("/find-by-original-login/{source}/{nameInSource}")
    fun findByOriginalLogin(
        @PathVariable source: String,
        @PathVariable nameInSource: String,
    ): Mono<AuthenticationUserDetailsResponse> = userService.findByOriginalLogin(nameInSource, source).map {
        ResponseEntity.ok().body(AuthenticationUserDetails(it))
    }

    companion object {
        private val roleForNewUser = listOf(Role.VIEWER).joinToString(",")
    }
}
