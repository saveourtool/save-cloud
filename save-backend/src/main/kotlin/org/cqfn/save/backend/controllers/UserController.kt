package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.UserDetailsService

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.security.jackson2.CoreJackson2Module
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Controller that returns data about users
 */
@RestController
@RequestMapping("/user")
class UserController(
    private val userDetailsService: UserDetailsService,
) {
    private val objectMapper = ObjectMapper().findAndRegisterModules()
        .registerModule(CoreJackson2Module())

    /**
     * @param username
     * @return serialized UserDetails
     */
    @GetMapping("/details")
    fun getUserDetails(@RequestParam username: String): Mono<String> =
            userDetailsService.findByUsername(username).map {
                // `CoreJackson2Module` pollutes jackson modules with bad serialization strategy for lists:
                // it adds some sort of type information, that confuses API consumers.
                // Hence, we need to use explicit serializer only for this controller.
                objectMapper.writeValueAsString(it)
            }
}
