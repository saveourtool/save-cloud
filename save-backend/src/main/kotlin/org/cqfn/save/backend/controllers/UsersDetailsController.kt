package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.domain.ImageInfo
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
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
     * @return [ImageInfo] about organization's avatar
     */
    @GetMapping("/{userName}/avatar")
    @PreAuthorize("permitAll()")
    fun avatar(@PathVariable userName: String): Mono<ImageInfo> = Mono.fromCallable {
        userRepository.findByName(userName).get().avatar
            .let { ImageInfo(it) }
    }
}
