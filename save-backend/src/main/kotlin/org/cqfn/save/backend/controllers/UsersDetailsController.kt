package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.StringResponse
import com.saveourtool.save.backend.repository.UserRepository
import com.saveourtool.save.backend.service.UserDetailsService
import com.saveourtool.save.backend.utils.AuthenticationDetails
import com.saveourtool.save.backend.utils.justOrNotFound
import com.saveourtool.save.domain.ImageInfo
import com.saveourtool.save.domain.Role
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.v1
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

/**
 * Controller that handles operation with users
 */
// TODO: https://github.com/analysis-dev/save-cloud/issues/656
@RestController
@RequestMapping(path = ["/api/$v1/users"])
class UsersDetailsController(
    private val userRepository: UserRepository,
    private val userDetailsService: UserDetailsService,
) {
    /**
     * @param userName username
     * @return [ImageInfo] about user's avatar
     */
    @GetMapping("/{userName}/avatar")
    @PreAuthorize("permitAll()")
    fun avatar(@PathVariable userName: String): Mono<ImageInfo> =
            justOrNotFound(userRepository.findByName(userName)).map { ImageInfo(it.avatar) }

    /**
     * @param userName username
     * @return [UserInfo] info about user's
     */
    @GetMapping("/{userName}")
    @PreAuthorize("permitAll()")
    fun findByName(@PathVariable userName: String): Mono<UserInfo> =
            justOrNotFound(userRepository.findByName(userName)).map { it.toUserInfo() }

    /**
     * @param newUserInfo
     * @param authentication an [Authentication] representing an authenticated request
     * @return response
     */
    @PostMapping("/save")
    @PreAuthorize("isAuthenticated()")
    fun saveUser(@RequestBody newUserInfo: UserInfo, authentication: Authentication): Mono<StringResponse> {
        val user = userRepository.findByName(newUserInfo.name).get()
        val userId = (authentication.details as AuthenticationDetails).id
        val response = if (user.id == userId) {
            userRepository.save(user.apply {
                email = newUserInfo.email
                company = newUserInfo.company
                location = newUserInfo.location
                gitHub = newUserInfo.gitHub
                linkedin = newUserInfo.linkedin
                twitter = newUserInfo.twitter
            })
            ResponseEntity.ok("User information saved successfully")
        } else {
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        return Mono.just(response)
    }

    /**
     * @param userName
     * @param token
     * @param authentication an [Authentication] representing an authenticated request
     * @return response
     */
    @PostMapping("{userName}/save/token")
    @PreAuthorize("isAuthenticated()")
    fun saveUserToken(@PathVariable userName: String, @RequestBody token: String, authentication: Authentication): Mono<StringResponse> {
        val user = userRepository.findByName(userName).get()
        val userId = (authentication.details as AuthenticationDetails).id
        val response = if (user.id == userId) {
            userRepository.save(user.apply {
                password = "{bcrypt}${BCryptPasswordEncoder().encode(token)}"
            })
            ResponseEntity.ok("User token saved successfully")
        } else {
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        return Mono.just(response)
    }

    /**
     * @param authentication
     * @return global [Role] of authenticated user
     */
    @GetMapping("/global-role")
    @PreAuthorize("isAuthenticated()")
    fun getSelfGlobalRole(authentication: Authentication): Mono<Role> =
            Mono.just(userDetailsService.getGlobalRole(authentication))
}
