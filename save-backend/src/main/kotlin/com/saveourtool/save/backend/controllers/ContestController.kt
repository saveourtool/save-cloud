package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.service.ContestService
import com.saveourtool.save.backend.service.TestService
import com.saveourtool.save.backend.service.TestSuitesService
import com.saveourtool.save.backend.utils.justOrNotFound
import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.test.PublicTestDto
import com.saveourtool.save.test.TestFilesRequest
import com.saveourtool.save.v1
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Controller for working with contests.
 */
@RestController
@RequestMapping(path = ["/api/$v1/contests"])
internal class ContestController(
    private val contestService: ContestService,
    private val testService: TestService,
    private val testSuitesService: TestSuitesService,
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
    @Suppress("UnsafeCallOnNullableType")
    fun getPublicTestForContest(
        @PathVariable contestName: String,
    ): Mono<PublicTestDto> {
        val contest = contestService.findByName(contestName).get()
        val testSuite = testSuitesService.findTestSuiteById(
            contest.testSuiteIds?.split(",")?.first()?.toLong()!!
        ).get()
        val test = testService.findTestsByTestSuiteId(testSuite.id!!).first()
        val testRequest = TestFilesRequest(test.toDto(), testSuite.testRootPath)
        return preprocessorWebClient.post().uri("/getTest").bodyValue(testRequest)
            .retrieve().bodyToMono(PublicTestDto::class.java)
    }
}
