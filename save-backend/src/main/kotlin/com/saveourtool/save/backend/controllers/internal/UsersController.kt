package com.saveourtool.save.backend.controllers.internal

import com.saveourtool.save.authservice.utils.IdentitySourceAwareUserDetails
import com.saveourtool.save.backend.repository.OriginalLoginRepository
import com.saveourtool.save.backend.service.UserDetailsService
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
    private val userService: UserDetailsService,
    private val originalLoginRepository: OriginalLoginRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper()
        .findAndRegisterModules()
        .registerModule(CoreJackson2Module())
        .addMixIn(IdentitySourceAwareUserDetails::class.java, IdentitySourceAwareUserDetailsMixin::class.java)

    /**
     * Stores user in the DB with provided [name] with [authorities] as role.
     * And add a link to [source] for created user
     *
     * @param source user source
     * @param name user name
     * @param authorities
     */
    @PostMapping("/new-if-required/{source}/{name}")
    @Transactional
    fun saveNewUserIfRequired(
        @PathVariable source: String,
        @PathVariable name: String,
        @RequestBody authorities: List<String>,
    ) {
        val userFind = originalLoginRepository.findByNameAndSource(name, source)

        userFind?.user?.let {
            logger.debug("User $name ($source) is already present in the DB")
        } ?: run {
            logger.info("Saving user $name ($source) with authorities $authorities to the DB")
            val savedUser = userService.saveNewUser(name, authorities.joinToString(","))
            userService.addSource(savedUser, name, source)
        }
    }

    /**
     * Find user by name
     *
     * @param userName user name
     * @return found [IdentitySourceAwareUserDetails] as a String
     */
    @GetMapping("/find-by-name/{userName}")
    fun findByName(
        @PathVariable userName: String,
    ): Mono<StringResponse> = userService.findByName(userName).map {
        ResponseEntity.ok().body(objectMapper.writeValueAsString(it))
    }

    /**
     * Find user by name and source
     *
     * @param source user source
     * @param nameInSource user name
     * @return found [IdentitySourceAwareUserDetails] as a String
     */
    @GetMapping("/find-by-original-login/{source}/{nameInSource}")
    fun findByOriginalLogin(
        @PathVariable source: String,
        @PathVariable nameInSource: String,
    ): Mono<StringResponse> = userService.findByOriginalLogin(nameInSource, source).map {
        ResponseEntity.ok().body(objectMapper.writeValueAsString(it))
    }
}
