package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.v1

import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

/**
 * Simple adaptor to the preprocessor. Frontend is able to communicate only with backend.
 * But the logic related to git is encapsulated in preprocessor. So we will simply forward this
 * request to a preprocessor.
 */
@RestController
@RequestMapping("/api/$v1/git")
class GitController(
    config: ConfigProperties,
) {
    private val webClientToPreprocessor = WebClient.create(config.preprocessorUrl)

    /**
     * @param gitDto
     * @return default branch name for provided credentials
     */
    @PostMapping("/default-branch-name")
    fun defaultBranchName(
        @RequestBody gitDto: GitDto
    ): Mono<String> = webClientToPreprocessor
        .post()
        .uri("/git/default-branch-name")
        .bodyValue(gitDto)
        .retrieve()
        .bodyToMono()
}
