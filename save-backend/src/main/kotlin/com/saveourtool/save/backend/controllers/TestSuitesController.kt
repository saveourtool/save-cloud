package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.scheduling.UpdateJob
import com.saveourtool.save.backend.service.TestSuitesService
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.v1

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
    private val quartzScheduler: Scheduler,
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
     * @param names list of test suite names
     * @return response with IDs of standard test suites with name from provided list
     */
    @PostMapping("/internal/findAllStandardTestSuiteIdsByName")
    fun getAllStandardTestSuiteIdsByName(@RequestBody names: List<String>) =
            ResponseEntity.status(HttpStatus.OK)
                .body(names.flatMap { name -> testSuitesService.findStandardTestSuitesByName(name) }
                    .map { it.requiredId() })

    /**
     * @param id id of the test suite
     * @return response with test suite with provided id
     */
    @GetMapping("/internal/testSuite/{id}")
    fun getTestSuiteById(@PathVariable id: Long) =
            ResponseEntity.status(HttpStatus.OK).body(testSuitesService.findTestSuiteById(id))

    /**
     * @param ids list of test suite ID
     * @return response with test suites with id from provided list
     */
    @PostMapping("/internal/findAllTestSuiteDtoByIds")
    fun findAllTestSuiteDtoByIds(@RequestBody ids: List<Long>) =
            ResponseEntity.status(HttpStatus.OK)
                .body(ids.map { id -> testSuitesService.findTestSuiteById(id).map { it.toDto() } })

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
