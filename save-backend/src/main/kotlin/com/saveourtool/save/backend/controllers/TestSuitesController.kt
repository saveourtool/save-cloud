package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.scheduling.UpdateJob
import com.saveourtool.save.backend.security.TestSuitePermissionEvaluator
import com.saveourtool.save.backend.service.TestSuiteDtoList
import com.saveourtool.save.backend.service.TestSuitesService
import com.saveourtool.save.domain.contestAllowedPlugins
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.testsuite.TestSuiteFilters
import com.saveourtool.save.v1

import org.quartz.Scheduler
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

typealias ResponseListTestSuites = ResponseEntity<List<TestSuiteDto>>

/**
 * Controller for test suites
 */
@RestController
class TestSuitesController(
    private val testSuitesService: TestSuitesService,
    private val quartzScheduler: Scheduler,
    private val testSuitePermissionEvaluator: TestSuitePermissionEvaluator,
) {
    /**
     * Save new test suites into DB
     *
     * @param testSuiteDtos
     * @return mono list of *all* TestSuite
     */
    @PostMapping("/internal/saveTestSuites")
    fun saveTestSuite(@RequestBody testSuiteDtos: List<TestSuiteDto>): Mono<List<TestSuite>> =
            Mono.just(testSuitesService.saveTestSuite(testSuiteDtos))

    /**
     * @return response with list of test suite dtos
     */
    @GetMapping(path = ["/api/$v1/allStandardTestSuites", "/internal/allStandardTestSuites"])
    fun getAllStandardTestSuites(): Mono<ResponseListTestSuites> =
            testSuitesService.getStandardTestSuites().map { ResponseEntity.status(HttpStatus.OK).body(it) }

    /**
     * @param testSuiteIds
     * @param authentication
     * @param isContest
     * @return [Flux] of [TestSuiteDto]s
     */
    @PostMapping("/api/$v1/test-suites/get-by-ids")
    fun getTestSuitesByIds(
        @RequestBody testSuiteIds: List<Long>,
        @RequestParam(required = false, defaultValue = "false") isContest: Boolean,
        authentication: Authentication,
    ): Flux<TestSuiteDto> = testSuitesService.findTestSuitesByIds(testSuiteIds)
        .mapToDtoFiltered(authentication, isContest)

    /**
     * @param organizationName
     * @param authentication
     * @param isContest
     * @return [Flux] of [TestSuiteDto]s
     */
    @GetMapping("/api/$v1/test-suites/get-by-organization")
    fun getTestSuitesByOrganizationName(
        @RequestParam organizationName: String,
        @RequestParam(required = false, defaultValue = "false") isContest: Boolean,
        authentication: Authentication,
    ): Flux<TestSuiteDto> = testSuitesService.findTestSuitesByOrganizationName(organizationName)
        .mapToDtoFiltered(authentication, isContest)

    /**
     * @param authentication
     * @param isContest
     * @return [Flux] of [TestSuiteDto]s
     */
    @GetMapping("/api/$v1/test-suites/get-standard")
    fun getStandardTestSuites(
        @RequestParam(required = false, defaultValue = "false") isContest: Boolean,
        authentication: Authentication,
    ): Mono<TestSuiteDtoList> = testSuitesService.getStandardTestSuites()
        .map { testSuites ->
            testSuites.filter {
                if (isContest) {
                    it.plugins == contestAllowedPlugins
                } else {
                    it.plugins.isNotEmpty()
                }
            }
        }

    /**
     * @param authentication
     * @param isContest
     * @return [Flux] of [TestSuiteDto]s
     */
    @GetMapping("/api/$v1/test-suites/available")
    fun getPublicTestSuites(
        @RequestParam(required = false, defaultValue = "false") isContest: Boolean,
        authentication: Authentication,
    ): Flux<TestSuiteDto> = testSuitesService.findAllTestSuites()
        .mapToDtoFiltered(authentication, isContest)

    private fun Flux<TestSuite>.mapToDtoFiltered(authentication: Authentication, isContest: Boolean): Flux<TestSuiteDto> = filter { testSuite ->
        testSuitePermissionEvaluator.canAccessTestSuite(testSuite, authentication)
    }
        .filter { testSuite ->
            if (isContest) {
                testSuite.pluginsAsListOfPluginType() == contestAllowedPlugins
            } else {
                testSuite.pluginsAsListOfPluginType().isNotEmpty()
            }
        }
        .map { testSuite ->
            testSuite.toDto(testSuite.requiredId())
        }

    /**
     * @param tags
     * @param name
     * @param language
     * @param authentication
     * @param isContest
     * @return [Flux] of [TestSuiteDto]s
     */
    @GetMapping("/api/$v1/test-suites/filtered")
    fun getFilteredTestSuites(
        @RequestParam(required = false, defaultValue = "") tags: String,
        @RequestParam(required = false, defaultValue = "") name: String,
        @RequestParam(required = false, defaultValue = "") language: String,
        @RequestParam(required = false, defaultValue = "false") isContest: Boolean,
        authentication: Authentication,
    ): Flux<TestSuiteDto> = Mono.just(TestSuiteFilters(name, language, tags))
        .flatMapMany {
            testSuitesService.findTestSuitesMatchingFilters(it)
        }
        .mapToDtoFiltered(authentication, isContest)

    /**
     * @param id id of the test suite
     * @return response with test suite with provided id
     */
    @GetMapping("/internal/testSuite/{id}")
    fun getTestSuiteById(@PathVariable id: Long) =
            ResponseEntity.status(HttpStatus.OK).body(testSuitesService.findTestSuiteById(id))

    /**
     * Trigger update of standard test suites. Can be called only by superadmins externally.
     *
     * @return response entity
     */
    @PostMapping(path = ["/api/$v1/updateStandardTestSuites", "/internal/updateStandardTestSuites"])
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    fun updateStandardTestSuites() = Mono.just(quartzScheduler)
        .map {
            it.triggerJob(
                UpdateJob.jobKey
            )
        }

    /**
     * @param testSuiteDtos suites, which need to be deleted
     * @return response entity
     */
    @PostMapping("/internal/deleteTestSuite")
    @Transactional
    fun deleteTestSuite(@RequestBody testSuiteDtos: List<TestSuiteDto>) =
            ResponseEntity.status(HttpStatus.OK).body(testSuitesService.deleteTestSuiteDto(testSuiteDtos))
}
