package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.domain.ImageInfo
import org.cqfn.save.info.UserInfo
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

/**
 * Controller that handles operation with users
 */
@RestController
@RequestMapping("/api/users")
class UsersDetailsController(
    private val userRepository: UserRepository,
) {
    /**
     * @param userName username
     * @return [ImageInfo] about user's avatar
     */
    @GetMapping("/{userName}/avatar")
    @PreAuthorize("permitAll()")
    fun avatar(@PathVariable userName: String): Mono<ImageInfo> = Mono.fromCallable {
        userRepository.findByName(userName).get().avatar
            .let { ImageInfo(it) }
    }

    /**
     * @param userName username
     * @return [UserInfo] info about user's
     */
    @GetMapping("/{userName}")
    @PreAuthorize("permitAll()")
    fun findByName(@PathVariable userName: String): Mono<UserInfo> = Mono.fromCallable {
        userRepository.findByName(userName).get().toUserInfo()
    }

    /**
     * @param newUserInfo
     */
    @PostMapping("/save")
    fun saveUser(@RequestBody newUserInfo: UserInfo) {
        val user = userRepository.findByName(newUserInfo.name).get()
        userRepository.save(user.apply {
            email = newUserInfo.email
        })
    }
}
