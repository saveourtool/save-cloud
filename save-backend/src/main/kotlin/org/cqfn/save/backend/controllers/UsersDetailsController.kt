package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.StringResponse
import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.backend.utils.justOrNotFound
import org.cqfn.save.domain.ImageInfo
import org.cqfn.save.info.UserInfo
import org.cqfn.save.v1

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
        println("${user.password}")
        val userId = (authentication.details as AuthenticationDetails).id
        val response = if (user.id == userId) {
            userRepository.save(user.apply {
                email = newUserInfo.email
                // TODO bcrypt
                password = newUserInfo.password
                company = newUserInfo.company
                location = newUserInfo.location
                gitHub = newUserInfo.gitHub
                linkedin = newUserInfo.linkedin
                twitter = newUserInfo.twitter
            })
            ResponseEntity.ok("User save")
        } else {
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        return Mono.just(response)
    }
}
