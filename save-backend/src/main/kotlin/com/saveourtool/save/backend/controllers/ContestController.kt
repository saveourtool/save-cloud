package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.service.ContestService
import com.saveourtool.save.backend.utils.justOrNotFound
import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.v1
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

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
}
