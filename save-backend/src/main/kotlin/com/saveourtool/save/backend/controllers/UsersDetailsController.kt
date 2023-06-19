package com.saveourtool.save.backend.controllers

import com.saveourtool.save.authservice.utils.AuthenticationDetails
import com.saveourtool.save.backend.repository.OriginalLoginRepository
import com.saveourtool.save.backend.repository.UserRepository
import com.saveourtool.save.backend.service.UserDetailsService
import com.saveourtool.save.backend.utils.toMonoOrNotFound
import com.saveourtool.save.domain.Role
import com.saveourtool.save.domain.UserSaveStatus
import com.saveourtool.save.entities.User
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.StringResponse
import com.saveourtool.save.utils.blockingToFlux
import com.saveourtool.save.utils.orNotFound
import com.saveourtool.save.utils.switchIfEmptyToResponseException
import com.saveourtool.save.v1

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Controller that handles operation with users
 */
// TODO: https://github.com/saveourtool/save-cloud/issues/656
@RestController
@RequestMapping(path = ["/api/$v1/users"])
class UsersDetailsController(
    private val userRepository: UserRepository,
    private val userDetailsService: UserDetailsService,
    private val originalLoginRepository: OriginalLoginRepository,
) {
    /**
     * @param userName username
     * @return [UserInfo] info about user's
     */
    @GetMapping("/{userName}")
    @PreAuthorize("permitAll()")
    fun findByName(@PathVariable userName: String): Mono<UserInfo> =
            userRepository.findByName(userName)
                ?.toMonoOrNotFound()?.map { it.toUserInfo() }
                ?: run {
                    originalLoginRepository.findByName(userName)
                        .toMonoOrNotFound()
                        .map { it.user }
                        .map { it.toUserInfo() }
                }

    /**
     * @return list of [UserInfo] info about user's
     */
    @GetMapping("/all")
    @PreAuthorize("permitAll()")
    fun findAll(): Flux<UserInfo> = blockingToFlux {
        userRepository.findAll().map { it.toUserInfo() }
    }

    /**
     * @param newUserInfo
     * @param authentication
     */
    @PostMapping("/save")
    @PreAuthorize("isAuthenticated()")
    fun saveUser(@RequestBody newUserInfo: UserInfo, authentication: Authentication): Mono<StringResponse> = Mono.just(newUserInfo)
        .map {
            val user: User = userRepository.findByName(newUserInfo.oldName ?: newUserInfo.name).orNotFound()
            val userId = (authentication.details as AuthenticationDetails).id
            val response = if (user.id == userId) {
                userDetailsService.saveUser(user.apply {
                    name = newUserInfo.name
                    email = newUserInfo.email
                    company = newUserInfo.company
                    location = newUserInfo.location
                    gitHub = newUserInfo.gitHub
                    linkedin = newUserInfo.linkedin
                    twitter = newUserInfo.twitter
                    isActive = newUserInfo.isActive
                }, newUserInfo.oldName)
            } else {
                UserSaveStatus.CONFLICT
            }
            response
        }
        .filter { status ->
            status == UserSaveStatus.UPDATE
        }
        .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
            UserSaveStatus.CONFLICT.message
        }
        .map { status ->
            ResponseEntity.ok(status.message)
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
        val user = userRepository.findByName(userName).orNotFound()
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

    /**
     * @param name
     * @return user
     */
    fun getByName(name: String): User? =
            userRepository.findByName(name) ?: run {
                originalLoginRepository.findByName(name)?.user
            }
}
