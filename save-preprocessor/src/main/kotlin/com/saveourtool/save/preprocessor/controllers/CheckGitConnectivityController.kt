package com.saveourtool.save.preprocessor.controllers

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
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
    ): Mono<Boolean> {
        log.info("Received a request to check git connectivity for $user: with $url")
        return Mono.just(
            try {
                Git.lsRemoteRepository()
                    .setCredentialsProvider(UsernamePasswordCredentialsProvider(user, token))
                    .setHeads(true)
                    .setTags(true)
                    .setRemote(url)
                    .call()
                true
            } catch (e: TransportException) {
                false
            }
        )
    }
}
