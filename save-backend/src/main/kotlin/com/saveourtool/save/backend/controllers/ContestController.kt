package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.configs.ApiSwaggerSupport
import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.save.backend.security.OrganizationPermissionEvaluator
import com.saveourtool.save.backend.service.ContestService
import com.saveourtool.save.backend.service.OrganizationService
import com.saveourtool.save.backend.service.TestService
import com.saveourtool.save.backend.utils.justOrNotFound
import com.saveourtool.save.entities.Contest.Companion.toContest
import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.test.TestFilesContent
import com.saveourtool.save.test.TestFilesRequest
import com.saveourtool.save.v1
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.util.*
import kotlin.jvm.optionals.getOrNull

/**
 * Controller for working with contests.
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "contests"),
)
@RestController
@OptIn(ExperimentalStdlibApi::class)
@RequestMapping(path = ["/api/$v1/contests"])
internal class ContestController(
    private val contestService: ContestService,
    private val testService: TestService,
    private val organizationPermissionEvaluator: OrganizationPermissionEvaluator,
    private val organizationService: OrganizationService,
    configProperties: ConfigProperties,
    jackson2WebClientCustomizer: WebClientCustomizer,
) {
    private val preprocessorWebClient = WebClient.builder()
        .apply(jackson2WebClientCustomizer::customize)
        .baseUrl(configProperties.preprocessorUrl)
        .build()

    @GetMapping("/{contestName}")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get contest by name.",
        description = "Get contest by name.",
    )
    @Parameters(
        Parameter(name = "contestName", `in` = ParameterIn.PATH, description = "name of a contest", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched contest by it's name.")
    @ApiResponse(responseCode = "404", description = "Contest with such name was not found.")
    fun getContestByName(@PathVariable contestName: String): Mono<ContestDto> = justOrNotFound(contestService.findByName(contestName))
        .map { it.toDto() }

    @GetMapping("/active")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get list of contests that are in progress now.",
        description = "Get list of contests that are in progress now.",
    )
    @Parameters(
        Parameter(name = "pageSize", `in` = ParameterIn.QUERY, description = "amount of contests that should be returned, default: 10", required = false),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched list of active contests.")
    fun getContestsInProgress(
        @RequestParam(defaultValue = "10") pageSize: Int,
        authentication: Authentication?,
    ): Flux<ContestDto> = Flux.fromIterable(
        contestService.findContestsInProgress(pageSize)
    ).map { it.toDto() }

    @GetMapping("/finished")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get list of contests that has already finished.",
        description = "Get list of contests that has already finished.",
    )
    @Parameters(
        Parameter(name = "pageSize", `in` = ParameterIn.QUERY, description = "amount of contests that should be returned, default: 10", required = false),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched list of finished contests.")
    fun getFinishedContests(
        @RequestParam(defaultValue = "10") pageSize: Int,
        authentication: Authentication?,
    ): Flux<ContestDto> = Flux.fromIterable(
        contestService.findFinishedContests(pageSize)
    ).map { it.toDto() }

    @GetMapping("/{contestName}/public-test")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get public test for contest with given name.",
        description = "Get public test for contest with given name.",
    )
    @Parameters(
        Parameter(name = "contestName", `in` = ParameterIn.PATH, description = "name of a contest", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched public tests.")
    @ApiResponse(responseCode = "404", description = "Either contest with such name was not found or tests are not provided.")
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
            testFilesContent.copy(
                language = testSuite.language,
            )
        }
    }

    @GetMapping("/by-organization")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get contests connected with given organization.",
        description = "Get contests connected with given organization.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.QUERY, description = "name of an organization", required = true),
        Parameter(name = "pageSize", `in` = ParameterIn.QUERY, description = "amount of records that will be returned", required = false),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched public tests.")
    @ApiResponse(responseCode = "404", description = "Either contest with such name was not found or tests are not provided.")
    fun getOrganizationContests(
        @RequestParam organizationName: String,
        @RequestParam(required = false, defaultValue = "10") pageSize: Int,
    ): Flux<ContestDto> = Flux.fromIterable(
        contestService.findPageOfContestsByOrganizationName(organizationName, Pageable.ofSize(pageSize))
    )
        .map { it.toDto() }

    @PostMapping("/create")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Create a new contest.",
        description = "Create a new contest.",
    )
    @Parameters(
        Parameter(name = "contestDto", `in` = ParameterIn.DEFAULT, description = "contest requested for creation", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Contest was successfully created.")
    @ApiResponse(responseCode = "404", description = "Organization with given name was not found.")
    @ApiResponse(responseCode = "403", description = "User cannot create contests with given organization.")
    @ApiResponse(responseCode = "409", description = "Contest with given name is already present.")
    @Suppress("TYPE_ALIAS")
    fun createContest(
        @RequestBody contestDto: ContestDto,
        authentication: Authentication,
    ): Mono<ResponseEntity<String>> = Mono.justOrEmpty(
        Optional.ofNullable(
            organizationService.findByName(contestDto.organizationName)
        )
    )
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }
        .filter {
            organizationPermissionEvaluator.canCreateContests(it, authentication)
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN))
        }
        .filter {
            contestService.createContestIfNotPresent(contestDto.toContest(it))
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(
                HttpStatus.CONFLICT,
                "Contest with name ${contestDto.name} is already present",
            ))
        }
        .map {
            ResponseEntity.ok("Contest has been successfully created!")
        }

    @PostMapping("/update")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Update contest.",
        description = "Change existing contest settings.",
    )
    @Parameters(
        Parameter(name = "contestRequest", `in` = ParameterIn.DEFAULT, description = "name of an organization", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched public tests.")
    @ApiResponse(responseCode = "403", description = "Not enough permission to edit current contest.")
    @ApiResponse(responseCode = "404", description = "Either organization or contest with such name was not found.")
    @Suppress("TYPE_ALIAS")
    fun updateContest(
        @RequestBody contestRequest: ContestDto,
        authentication: Authentication,
    ): Mono<ResponseEntity<String>> = Mono.zip(
        Mono.justOrEmpty(Optional.ofNullable(organizationService.findByName(contestRequest.organizationName))),
        Mono.justOrEmpty(contestService.findByName(contestRequest.name)),
    )
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Either organization [${contestRequest.organizationName}] or contest [${contestRequest.name}] was not found."))
        }
        .filter { (organization, _) ->
            organizationPermissionEvaluator.hasPermission(authentication, organization, Permission.DELETE)
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have enough permissions to edit this contest."))
        }
        .map { (organization, contest) ->
            contestService.updateContest(
                contestRequest.toContest(organization, contest.testSuiteIds, contest.status).apply { id = contest.id }
            )
            ResponseEntity.ok("Contest successfully updated")
        }
}
