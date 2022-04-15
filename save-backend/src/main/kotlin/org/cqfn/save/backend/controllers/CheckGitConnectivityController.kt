package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.configs.ConfigProperties
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

/**
 * Controller for checking git credentials
 */
@RestController
@RequestMapping("/api")
class CheckGitConnectivityController(
    config: ConfigProperties,
) {
    private val webClientToPreprocessor = WebClient.create(config.preprocessorUrl)

    /**
     * Simple adaptor to the preprocessor. Frontend is able to communicate only with backend.
     * But the logic related to git is encapsulated in preprocessor. So we will simply forward this
     * request to a preprocessor.
     *
     * @param user git user
     * @param token git password
     * @param url repository url
     * @return true if credentials are valid
     */
    @GetMapping(value = ["/check-git-connectivity-adaptor"])
    fun checkGitConnectivity(
        @RequestParam user: String,
        @RequestParam token: String,
        @RequestParam url: String,
    ): Mono<Boolean> {
        if (user.isBlank() || token.isBlank() || url.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
        return webClientToPreprocessor
            .get()
            .uri("/check-git-connectivity?user=$user&token=$token&url=$url")
            .retrieve()
            .bodyToMono(Boolean::class.java)
    }
}
