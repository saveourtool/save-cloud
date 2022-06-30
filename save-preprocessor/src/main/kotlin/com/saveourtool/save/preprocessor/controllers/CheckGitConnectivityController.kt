package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.save.preprocessor.utils.detectLatestSha1
import com.saveourtool.save.testsuite.GitLocation

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

/**
 * Controller for checking git credentials
 */
@RestController
class CheckGitConnectivityController {
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

    /**
     * @param gitLocation
     */
    @PostMapping("/git/detect-latest-sha1")
    fun detectLatestSha1(
        @RequestBody gitLocation: GitLocation
    ): Mono<String> {
        log.info("Received a request to detect latest sha1 for $gitLocation")
        return Mono.just(
            gitLocation.detectLatestSha1()
        )
    }

    /**
     * @param gitLocation
     * @return default branch name
     */
    @PostMapping("/git/detect-default-branch-name")
    fun detectDefaultBranchName(
        @RequestBody gitLocation: GitLocation
    ): Mono<String> {
        log.info("Received a request to detect latest sha1 for $gitLocation")
        return Mono.just(
            gitLocation.detectLatestSha1()
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(CheckGitConnectivityController::class.java)
    }
}
