package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.backend.utils.justOrNotFound
import org.cqfn.save.domain.ImageInfo
import org.cqfn.save.info.UserInfo
import org.cqfn.save.v1

import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

/**
 * Controller that handles operation with users
 */
@RestController
@RequestMapping(path = ["/api/$v1/users"])
class UsersDetailsController(
    private val userRepository: UserRepository,
) {
    /**
     * @param userName username
     * @return [ImageInfo] about user's avatar
     */
    @GetMapping("/{userName}/avatar")
    @PreAuthorize("permitAll()")
    fun avatar(@PathVariable userName: String): Mono<ImageInfo> =
            justOrNotFound(userRepository.findByName(userName)).map { it.avatar }.map { ImageInfo(it) }

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
     * @throws ResponseStatusException
     */
    @PostMapping("/save")
    @PreAuthorize("isAuthenticated()")
    fun saveUser(@RequestBody newUserInfo: UserInfo, authentication: Authentication) {
        val user = userRepository.findByName(newUserInfo.name).get()
        val userId = (authentication.details as AuthenticationDetails).id
        if (user.id == userId) {
            userRepository.save(user.apply {
                email = newUserInfo.email
                company = newUserInfo.company
                company = newUserInfo.location
                company = newUserInfo.gitHub
                company = newUserInfo.linkedin
                company = newUserInfo.twitter
            })
        } else {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
    }
}
