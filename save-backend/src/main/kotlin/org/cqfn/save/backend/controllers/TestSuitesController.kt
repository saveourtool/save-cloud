package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.scheduling.StandardSuitesUpdateScheduler
import org.cqfn.save.backend.service.TestSuitesService
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.testsuite.TestSuiteDto
import org.quartz.JobKey
import org.quartz.Scheduler

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

typealias ResponseListTestSuites = ResponseEntity<List<TestSuiteDto>>

/**
 * Controller for test suites
 */
@RestController
class TestSuitesController(
    private val testSuitesService: TestSuitesService,
    private val scheduler: Scheduler,
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
    @GetMapping(path = ["/api/allStandardTestSuites", "/internal/allStandardTestSuites"])
    fun getAllStandardTestSuites(): ResponseListTestSuites =
            ResponseEntity.status(HttpStatus.OK).body(testSuitesService.getStandardTestSuites())

    /**
     * @param name name of the test suite
     * @return response with list of test suite with specific name
     */
    @GetMapping("/internal/standardTestSuitesWithName")
    fun getAllStandardTestSuitesWithSpecificName(@RequestParam name: String) =
            ResponseEntity.status(HttpStatus.OK).body(testSuitesService.findStandardTestSuitesByName(name))

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
    @PostMapping(path = ["/api/updateStandardTestSuites", "/internal/updateStandardTestSuites"])
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    fun updateStandardTestSuites() = Mono.fromCallable {
        scheduler.triggerJob(
            JobKey.jobKey(StandardSuitesUpdateScheduler.jobName)
        )
    }

    /**
     * @param testSuiteDtos suites, which need to be marked as obsolete
     * @return response entity
     */
    @PostMapping("/internal/markObsoleteTestSuites")
    @Transactional
    fun markObsoleteTestSuites(@RequestBody testSuiteDtos: List<TestSuiteDto>) =
            ResponseEntity.status(HttpStatus.OK).body(testSuitesService.markObsoleteTestSuites(testSuiteDtos))

    /**
     * @param testSuiteDtos suites, which need to be deleted
     * @return response entity
     */
    @PostMapping("/internal/deleteTestSuite")
    @Transactional
    fun deleteTestSuite(@RequestBody testSuiteDtos: List<TestSuiteDto>) =
            ResponseEntity.status(HttpStatus.OK).body(testSuitesService.deleteTestSuiteDto(testSuiteDtos))
}
