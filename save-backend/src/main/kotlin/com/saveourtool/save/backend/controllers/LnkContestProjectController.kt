/**
 * Controller for processing links between users and their roles in organizations:
 * 1) to put new roles of users
 * 2) to get users and their roles by organization
 * 3) to remove users from organizations
 */

package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.service.LnkContestProjectService
import com.saveourtool.save.entities.ContestResult
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
 * Controller for processing links between users and their roles in organizations
 */
@RestController
@RequestMapping("/api/$v1/contests/")
class LnkContestProjectController(
    private val lnkContestProjectService: LnkContestProjectService,
) {
    /**
     * @param contestName
     * @param projectName
     * @param authentication
     * @return score of a project with [projectName] in contest with [contestName]
     * @throws ResponseStatusException
     */
    @GetMapping("{contestName}/{projectName}/scores")
    fun getProjectRatingInContest(
        @PathVariable contestName: String,
        @PathVariable projectName: String,
        authentication: Authentication,
    ): Mono<Float> = Mono.justOrEmpty(
        lnkContestProjectService.getByContestNameAndProjectName(contestName, projectName)
    )
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }
        .map { it.score }

    /**
     * @param contestName
     * @param authentication
     * @return score of all projects in contest with [contestName]
     * @throws ResponseStatusException
     */
    @GetMapping("/{contestName}/scores")
    fun getRatingsInContest(
        @PathVariable contestName: String,
        authentication: Authentication,
    ): Flux<ContestResult> = Flux.fromIterable(
        lnkContestProjectService.getByContestName(contestName)
    )
        .map {
            it.toContestResult()
        }

    /**
     * @param organizationName
     * @param projectName
     * @param numberOfContests
     * @return best [numberOfContests] contests of a project with name [projectName]
     */
    @GetMapping("/{organizationName}/{projectName}/best")
    @PreAuthorize("permitAll()")
    fun getBestProjectsContests(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam(defaultValue = "4") numberOfContests: Int,
    ): Flux<ContestResult> = Flux.fromIterable(
        lnkContestProjectService.getBestContestsByProject(projectName, organizationName, numberOfContests)
    )
        .map {
            it.toContestResult()
        }
}
