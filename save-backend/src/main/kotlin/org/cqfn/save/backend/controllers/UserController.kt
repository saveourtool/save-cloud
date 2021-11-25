package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.UserDetailsService

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/user")
class UserController(
    private val userDetailsService: UserDetailsService,
) {
    @GetMapping("/details")
    fun getUserDetails(@RequestParam username: String): Mono<UserDetails> =
        userDetailsService.findByUsername(username)
}
