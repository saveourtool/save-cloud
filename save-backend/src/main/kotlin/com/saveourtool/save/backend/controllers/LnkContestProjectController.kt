/**
 * Controller for processing links between users and their roles in organizations:
 * 1) to put new roles of users
 * 2) to get users and their roles by organization
 * 3) to remove users from organizations
 */

package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.security.ProjectPermissionEvaluator
import com.saveourtool.save.backend.service.*
import com.saveourtool.save.backend.utils.AuthenticationDetails
import com.saveourtool.save.entities.ContestResult
import com.saveourtool.save.entities.LnkContestProject
import com.saveourtool.save.entities.Project
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.v1

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

/**
 * Controller for processing links between projects and contests with scores
 */
@RestController
@RequestMapping("/api/$v1/contests/")
class LnkContestProjectController(
    private val lnkContestProjectService: LnkContestProjectService,
    private val lnkContestExecutionService: LnkContestExecutionService,
    private val lnkUserProjectService: LnkUserProjectService,
    private val projectPermissionEvaluator: ProjectPermissionEvaluator,
    private val contestService: ContestService,
    private val projectService: ProjectService,
) {
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
    ): Flux<ContestResult> = Flux.fromIterable(lnkContestProjectService.getAllByContestName(contestName))
        .getScores()

    /**
     * @param organizationName
     * @param projectName
     * @param amount
     * @param authentication
     * @return best [amount] contests of a project with name [projectName]
     */
    @GetMapping("/{organizationName}/{projectName}/best")
    @PreAuthorize("permitAll()")
    fun getBestProjectContests(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam(defaultValue = "4") amount: Int,
        authentication: Authentication,
    ): Flux<ContestResult> = Flux.fromIterable(
        lnkContestProjectService.getByProjectNameAndOrganizationName(projectName, organizationName, amount)
    ).getScores()

    private fun Flux<LnkContestProject>.getScores() = map {
        it to lnkContestExecutionService.getBestScoreOfProjectInContestWithName(it.project, it.contest.name)
    }
        .filter { (_, score) ->
            score != null
        }
        .map { (lnkContestProject, score) ->
            lnkContestProject.toContestResult(score)
        }

    /**
     * @param contestName
     * @param authentication
     * @return list of user's [Project]s that can participate in contest with name [contestName]
     */
    @GetMapping("/{contestName}/eligible-projects")
    fun getAvaliableProjectsForContest(
        @PathVariable contestName: String,
        authentication: Authentication,
    ): Mono<List<String>> = Mono.fromCallable {
        lnkUserProjectService.getAllProjectsByUserId((authentication.details as AuthenticationDetails).id).filter { it.public }
    }
        .map { userProjects ->
            userProjects to lnkContestProjectService.getProjectsFromListAndContest(contestName, userProjects).map { it.project }
        }
        .map { (projects, projectsFromContest) ->
            projects.minus(projectsFromContest.toSet()).map { "${it.organization.name}/${it.name}" }
        }

    /**
     * @param organizationName
     * @param projectName
     * @param authentication
     * @return list of active contests' names that project with name [projectName] and from organization with name [organizationName] can participate
     */
    @GetMapping("/{organizationName}/{projectName}/eligible-contests")
    fun getAvaliableContestsForProject(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        authentication: Authentication,
    ): Mono<List<String>> = Mono.fromCallable {
        lnkContestProjectService.getByProjectNameAndOrganizationName(projectName, organizationName, MAX_AMOUNT)
            .mapNotNull { it.contest.id }
    }
        .map { it.toSet() }
        .map { contestIds ->
            if (contestIds.isEmpty()) {
                contestService.findContestsInProgress(MAX_AMOUNT).map { it.name }
            } else {
                contestService.getAllActiveContestsNotFrom(contestIds, MAX_AMOUNT).map { it.name }
            }
        }

    /**
     * @param contestName
     * @param projectName
     * @param organizationName
     * @param authentication
     * @return [String] with response
     */
    @GetMapping("/{contestName}/enroll")
    @PreAuthorize("permitAll()")
    @Suppress("TYPE_ALIAS", "UnsafeCallOnNullableType")
    fun enrollForContest(
        @PathVariable contestName: String,
        @RequestParam projectName: String,
        @RequestParam organizationName: String,
        authentication: Authentication,
    ): Mono<ResponseEntity<String>> = Mono.zip(
        projectService.findWithPermissionByNameAndOrganization(
            authentication,
            projectName,
            organizationName,
            Permission.READ,
            "No such project found or not enough permission for this project",
            HttpStatus.NOT_FOUND,
        ),
        Mono.justOrEmpty(contestService.findByName(contestName)),
    )
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }
        .filter { (project, _) ->
            projectPermissionEvaluator.hasPermission(authentication, project, Permission.WRITE)
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN))
        }
        .filter { (project, _) ->
            project.public
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.CONFLICT, "Private projects cannot be enrolled in the contest"))
        }
        .map { (project, contest) ->
            lnkContestProjectService.saveLnkContestProject(project, contest)
        }
        .map {
            if (it) {
                ResponseEntity.ok("You have successfully enrolled for this contest!")
            } else {
                ResponseEntity.ok("You are already enrolled for this contest.")
            }
        }

    companion object {
        const val MAX_AMOUNT = 512
    }
}
