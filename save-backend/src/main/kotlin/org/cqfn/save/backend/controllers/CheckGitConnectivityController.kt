package org.cqfn.save.backend.controllers

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class CheckGitConnectivityController {
    private val log = LoggerFactory.getLogger(CheckGitConnectivityController::class.java)

    /**
     * @param user
     * @param token
     * @param url
     * @return
     */
    @GetMapping(value = ["/checkGitConnectivity"])
    fun checkGitConnectivity(
        @RequestParam user: String,
        @RequestParam token: String,
        @RequestParam url: String,
    ): Mono<Boolean> {
        log.info("Received a request to check git connectivity for $user: with $url")
        return try {
            Git.lsRemoteRepository()
                .setCredentialsProvider(UsernamePasswordCredentialsProvider(user, token))
                .setHeads(true)
                .setTags(true)
                .setRemote(url)
                .call()
            Mono.just(true)
        } catch (e: TransportException) {
            Mono.just(false)
        }
    }
}
