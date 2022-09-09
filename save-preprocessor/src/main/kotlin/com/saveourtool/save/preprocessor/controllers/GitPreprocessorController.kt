package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.preprocessor.utils.detectDefaultBranchName
import com.saveourtool.save.preprocessor.utils.detectTagList
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.info
import org.slf4j.Logger
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

typealias StringCollection = Collection<String>

/**
 * Controller for checking git credentials
 */
@RestController
@RequestMapping("/git")
class GitPreprocessorController {
    /**
     * Endpoint used to check git credentials and git url
     *
     * @param gitDto selected git credentials
     * @return true if credentials are valid
     */
    @PostMapping(value = ["/check-connectivity"])
    fun checkConnectivity(
        @RequestBody gitDto: GitDto
    ): Mono<Boolean> = blockingToMono {
        log.info { "Received a request to check git connectivity for ${gitDto.username} with ${gitDto.url}" }
        try {
            // a simple operation by detecting a default branch
            gitDto.detectDefaultBranchName()
            true
        } catch (e: IllegalStateException) {
            false
        }
    }

    /**
     * Detect a default branch name for a specific git coordinate
     *
     * @param gitDto git coordinate which is selected for test suites source
     * @return detected default branch name
     */
    @PostMapping("/default-branch-name")
    fun defaultBranchName(
        @RequestBody gitDto: GitDto
    ): Mono<String> = blockingToMono {
        log.info { "Received a request to detect a default branch name in ${gitDto.url}" }
        gitDto.detectDefaultBranchName()
    }

    /**
     * Detect a list of tags for a specific git coordinate
     *
     * @param gitDto git coordinate which is selected for test suites source
     * @return detected list of tags
     */
    @PostMapping("/tag-list")
    fun tagList(
        @RequestBody gitDto: GitDto,
    ): Mono<StringCollection> = blockingToMono {
        log.info { "Received a request to detect a list of tags in ${gitDto.url}" }
        gitDto.detectTagList()
    }

    companion object {
        private val log: Logger = getLogger<GitPreprocessorController>()
    }
}
