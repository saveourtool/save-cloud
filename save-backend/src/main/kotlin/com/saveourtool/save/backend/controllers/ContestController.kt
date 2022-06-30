package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.service.ContestService
import com.saveourtool.save.entities.Contest
import com.saveourtool.save.v1
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * Controller for working with contests.
 */
@RestController
@RequestMapping(path = ["/api/$v1/contests"])
internal class ContestController(
    private val contestService: ContestService,
) {
    /**
     * @param contestName
     * @return Organization
     */
    @GetMapping("/{contestName}")
    @PreAuthorize("permitAll()")
    fun getContestByName(@PathVariable contestName: String) = Mono.fromCallable {
        contestService.findByName(contestName)
    }.switchIfEmpty {
        Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
    }

    /**
     * @param pageSize amount of contests that should be taken
     * @param authentication an [Authentication] representing an authenticated request
     * @return list of organization by owner id
     */
    @GetMapping("/get/active")
    @PreAuthorize("permitAll()")
    fun getContestsInProgress(
        @RequestParam(required = false) pageSize: Int? = null,
        authentication: Authentication?,
    ): Flux<Contest> = Flux.fromIterable(
        contestService.findContestsInProgress(pageSize)
    )

    /**
     * @param pageSize amount of contests that should be taken
     * @param authentication an [Authentication] representing an authenticated request
     * @return list of organization by owner id
     */
    @GetMapping("/finished")
    @PreAuthorize("permitAll()")
    fun getFinishedContests(
        @RequestParam(required = false) pageSize: Int? = null,
        authentication: Authentication?,
    ): Flux<Contest> = Flux.fromIterable(
        contestService.findFinishedContests(pageSize)
    )
}
