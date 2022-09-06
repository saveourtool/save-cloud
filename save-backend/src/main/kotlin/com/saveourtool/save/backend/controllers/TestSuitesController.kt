package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.configs.ApiSwaggerSupport
import com.saveourtool.save.backend.scheduling.UpdateJob
import com.saveourtool.save.backend.security.TestSuitePermissionEvaluator
import com.saveourtool.save.backend.service.TestSuiteDtoList
import com.saveourtool.save.backend.service.TestSuitesService
import com.saveourtool.save.domain.isAllowedForContests
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.testsuite.TestSuiteFilters
import com.saveourtool.save.v1
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags

import org.quartz.Scheduler
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

typealias ResponseListTestSuites = ResponseEntity<List<TestSuiteDto>>

/**
 * Controller for test suites
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "test-suites"),
)
@RestController
class TestSuitesController(
    private val testSuitesService: TestSuitesService,
    private val quartzScheduler: Scheduler,
    private val testSuitePermissionEvaluator: TestSuitePermissionEvaluator,
) {
    @PostMapping("/internal/saveTestSuites")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Save test suites.",
        description = "Save new test suites into DB.",
    )
    @Tag(name = "internal")
    @ApiResponse(responseCode = "200", description = "Successfully saved test suites.")
    fun saveTestSuite(@RequestBody testSuiteDtos: List<TestSuiteDto>): Mono<List<TestSuite>> =
            Mono.just(testSuiteDtos)
                .filter { it.isNotEmpty() }
                .map { testSuitesService.saveTestSuite(it) }
                .defaultIfEmpty(emptyList())

    @PostMapping("/api/$v1/test-suites/get-by-ids")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Get test suites by ids.",
        description = "Get list of available test suites by their ids.",
    )
    @Parameters(
        Parameter(name = "isContest", `in` = ParameterIn.QUERY, description = "is given request sent for browsing test suites for contest, default is false", required = false),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched test suites by ids.")
    fun getTestSuitesByIds(
        @RequestBody testSuiteIds: List<Long>,
        @RequestParam(required = false, defaultValue = "false") isContest: Boolean,
        authentication: Authentication,
    ): Flux<TestSuiteDto> = testSuitesService.findTestSuitesByIds(testSuiteIds)
        .toFlux()
        .mapToDtoFiltered(authentication, isContest)

    @GetMapping("/api/$v1/test-suites/get-by-organization")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get test suites by organization.",
        description = "Get list of available test suites posted by given organization.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.QUERY, description = "name of an organization", required = true),
        Parameter(name = "isContest", `in` = ParameterIn.QUERY, description = "is given request sent for browsing test suites for contest, default is false", required = false),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched test suites by organization name.")
    fun getTestSuitesByOrganizationName(
        @RequestParam organizationName: String,
        @RequestParam(required = false, defaultValue = "false") isContest: Boolean,
        authentication: Authentication,
    ): Flux<TestSuiteDto> = testSuitesService.findTestSuitesByOrganizationName(organizationName)
        .toFlux()
        .mapToDtoFiltered(authentication, isContest)

    @GetMapping("/api/$v1/test-suites/get-standard")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get standard test suites.",
        description = "Get list of standard test suites.",
    )
    @Parameters(
        Parameter(name = "isContest", `in` = ParameterIn.QUERY, description = "is given request sent for browsing test suites for contest, default is false", required = false),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched standard test suites.")
    fun getStandardTestSuites(
        @RequestParam(required = false, defaultValue = "false") isContest: Boolean,
        authentication: Authentication,
    ): Mono<TestSuiteDtoList> = testSuitesService.getStandardTestSuites()
        .map { testSuites ->
            testSuites.filter {
                if (isContest) {
                    it.plugins.isAllowedForContests()
                } else {
                    it.plugins.isNotEmpty()
                }
            }
        }

    @GetMapping("/api/$v1/test-suites/available")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get public test suites.",
        description = "Get list of available test suites.",
    )
    @Parameters(
        Parameter(name = "isContest", `in` = ParameterIn.QUERY, description = "is given request sent for browsing test suites for contest, default is false", required = false),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched public test suites.")
    fun getPublicTestSuites(
        @RequestParam(required = false, defaultValue = "false") isContest: Boolean,
        authentication: Authentication,
    ): Flux<TestSuiteDto> = testSuitesService.findAllTestSuites().toFlux()
        .mapToDtoFiltered(authentication, isContest)

    private fun Flux<TestSuite>.mapToDtoFiltered(authentication: Authentication, isContest: Boolean): Flux<TestSuiteDto> = filter { testSuite ->
        testSuitePermissionEvaluator.canAccessTestSuite(testSuite, authentication)
    }
        .filter { testSuite ->
            if (isContest) {
                testSuite.pluginsAsListOfPluginType().isAllowedForContests()
            } else {
                testSuite.pluginsAsListOfPluginType().isNotEmpty()
            }
        }
        .map { testSuite ->
            testSuite.toDto(testSuite.requiredId())
        }

    @GetMapping("/api/$v1/test-suites/filtered")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get test suites with filters.",
        description = "Get test suites with filters.",
    )
    @Parameters(
        Parameter(name = "tags", `in` = ParameterIn.QUERY, description = "test suite tags substring for filtering, default is empty", required = false),
        Parameter(name = "name", `in` = ParameterIn.QUERY, description = "test suite name substring for filtering, default is empty", required = false),
        Parameter(name = "language", `in` = ParameterIn.QUERY, description = "test suite language substring for filtering, default is empty", required = false),
        Parameter(name = "isContest", `in` = ParameterIn.QUERY, description = "is given request sent for browsing test suites for contest, default is false", required = false),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched filtered test suites.")
    fun getFilteredTestSuites(
        @RequestParam(required = false, defaultValue = "") tags: String,
        @RequestParam(required = false, defaultValue = "") name: String,
        @RequestParam(required = false, defaultValue = "") language: String,
        @RequestParam(required = false, defaultValue = "false") isContest: Boolean,
        authentication: Authentication,
    ): Flux<TestSuiteDto> = Mono.just(TestSuiteFilters(name, language, tags))
        .flatMapMany {
            testSuitesService.findTestSuitesMatchingFilters(it).toFlux()
        }
        .mapToDtoFiltered(authentication, isContest)

    @GetMapping("/internal/testSuite/{id}")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get test suite by id.",
        description = "Get test suite by id.",
    )
    @Tag(name = "internal")
    @Parameters(
        Parameter(name = "id", `in` = ParameterIn.PATH, description = "id of test suite", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched filtered test suites.")
    fun getTestSuiteById(@PathVariable id: Long): ResponseEntity<TestSuite?> =
            ResponseEntity.status(HttpStatus.OK).body(testSuitesService.findTestSuiteById(id))

    @PostMapping(path = ["/api/$v1/updateStandardTestSuites", "/internal/updateStandardTestSuites"])
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @Tags(
        Tag(name = "superadmins"),
        Tag(name = "internal"),
    )
    @Operation(
        method = "POST",
        summary = "Update standard test suites.",
        description = "Trigger update of standard test suites. Can be called only by super admins externally.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully updated standard test suites.")
    fun updateStandardTestSuites(): Mono<Unit> = Mono.just(quartzScheduler)
        .map {
            it.triggerJob(
                UpdateJob.jobKey
            )
        }

    @PostMapping("/internal/deleteTestSuite")
    @Transactional
    @Tag(name = "internal")
    @Operation(
        method = "POST",
        summary = "Delete test suites.",
        description = "Delete test suites.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully deleted test suites.")
    fun deleteTestSuite(@RequestBody testSuiteDtos: List<TestSuiteDto>): ResponseEntity<Unit> =
            ResponseEntity.status(HttpStatus.OK).body(testSuitesService.deleteTestSuiteDto(testSuiteDtos))
}
