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
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.web.bind.annotation.*
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
        val userId = (authentication.details as AuthenticationDetails).id
        val response = if (user.id == userId) {
            userRepository.save(user.apply {
                email = newUserInfo.email.getValueOrNull() ?: email
                password = "{bcrypt}${BCrypt.hashpw(newUserInfo.password.getValueOrNull() ?: password, BCrypt.gensalt())}"
                company = newUserInfo.company.getValueOrNull() ?: company
                location = newUserInfo.location.getValueOrNull() ?: location
                gitHub = newUserInfo.gitHub.getValueOrNull() ?: gitHub
                linkedin = newUserInfo.linkedin.getValueOrNull() ?: linkedin
                twitter = newUserInfo.twitter.getValueOrNull() ?: twitter
            })
            ResponseEntity.ok("User saved successfully")
        } else {
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        return Mono.just(response)
    }

    private fun String?.getValueOrNull(): String? = if (!this.isNullOrBlank()) {
        this
    } else {
        null
    }
}
