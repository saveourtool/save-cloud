package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.preprocessor.utils.detectDefaultBranchName
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Controller for checking git credentials
 */
@RestController
class CheckGitConnectivityController {
    private val log = LoggerFactory.getLogger(CheckGitConnectivityController::class.java)

    /**
     * Endpoint used to check git credentials and git url
     *
     * @param user git user
     * @param token git password
     * @param url repository url
     * @return true if credentials are valid
     */
    @GetMapping(value = ["/check-git-connectivity"])
    fun checkGitConnectivity(
        @RequestParam user: String,
        @RequestParam token: String,
        @RequestParam url: String,
    ): Mono<Boolean> = Mono.fromCallable {
        log.info("Received a request to check git connectivity for $user: with $url")
        try {
            // a simple operation by detecting a default branch
            GitDto(
                url,
                user,
                token,
            ).detectDefaultBranchName()
            true
        } catch (e: IllegalStateException) {
            false
        }
    }
}
