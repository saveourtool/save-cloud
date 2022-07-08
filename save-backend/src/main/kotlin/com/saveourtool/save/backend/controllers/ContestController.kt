package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.service.ContestService
import com.saveourtool.save.backend.service.TestService
import com.saveourtool.save.backend.utils.justOrNotFound
import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.test.TestFilesContent
import com.saveourtool.save.test.TestFilesRequest
import com.saveourtool.save.v1
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.jvm.optionals.getOrNull

/**
 * Controller for working with contests.
 */
@RestController
@OptIn(ExperimentalStdlibApi::class)
@RequestMapping(path = ["/api/$v1/contests"])
internal class ContestController(
    private val contestService: ContestService,
    private val testService: TestService,
    configProperties: ConfigProperties,
    jackson2WebClientCustomizer: WebClientCustomizer,
) {
    private val preprocessorWebClient = WebClient.builder()
        .apply(jackson2WebClientCustomizer::customize)
        .baseUrl(configProperties.preprocessorUrl)
        .build()

    /**
     * @param contestName
     * @return Organization
     */
    @GetMapping("/{contestName}")
    @PreAuthorize("permitAll()")
    fun getContestByName(@PathVariable contestName: String): Mono<ContestDto> = justOrNotFound(contestService.findByName(contestName))
        .map { it.toDto() }

    /**
     * @param pageSize amount of contests that should be taken
     * @param authentication an [Authentication] representing an authenticated request
     * @return list of organization by owner id
     */
    @GetMapping("/active")
    @PreAuthorize("permitAll()")
    fun getContestsInProgress(
        @RequestParam(defaultValue = "10") pageSize: Int,
        authentication: Authentication?,
    ): Flux<ContestDto> = Flux.fromIterable(
        contestService.findContestsInProgress(pageSize)
    ).map { it.toDto() }

    /**
     * @param pageSize amount of contests that should be taken
     * @param authentication an [Authentication] representing an authenticated request
     * @return list of organization by owner id
     */
    @GetMapping("/finished")
    @PreAuthorize("permitAll()")
    fun getFinishedContests(
        @RequestParam(defaultValue = "10") pageSize: Int,
        authentication: Authentication?,
    ): Flux<ContestDto> = Flux.fromIterable(
        contestService.findFinishedContests(pageSize)
    ).map { it.toDto() }

    /**
     * @param contestName
     * @return Organization
     */
    @GetMapping("/{contestName}/public-test")
    @PreAuthorize("permitAll()")
    fun getPublicTestForContest(
        @PathVariable contestName: String,
    ): Mono<TestFilesContent> {
        val contest = contestService.findByName(contestName).getOrNull()
            ?: return Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        val testSuite = contestService.getTestSuiteForPublicTest(contest)
            ?: return Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        val test = testService.findTestsByTestSuiteId(testSuite.requiredId()).firstOrNull()
            ?: return Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        return TestFilesRequest(test.toDto(), testSuite.testRootPath).let<TestFilesRequest, Mono<TestFilesContent>> {
            preprocessorWebClient.post()
                .uri("/tests/get-content")
                .bodyValue(it)
                .retrieve()
                .bodyToMono()
        }.map { testFilesContent ->
            testFilesContent.apply {
                language = testSuite.language
                tags = test.tagsAsList() ?: emptyList()
            }
        }
    }
}
