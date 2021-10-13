package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.backend.scheduling.StandardSuitesUpdateScheduler
import org.cqfn.save.backend.service.TestSuitesService
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.testsuite.TestSuiteDto
import org.quartz.JobKey
import org.quartz.Scheduler
import org.slf4j.LoggerFactory

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

typealias ResponseListTestSuites = ResponseEntity<List<TestSuiteDto>>

/**
 * Controller for test suites
 */
@RestController
class TestSuitesController(
    private val configProperties: ConfigProperties,
    private val scheduler: Scheduler,
) {
    private val log = LoggerFactory.getLogger(TestSuitesController::class.java)
    private val preprocessorWebClient = WebClient.create(configProperties.preprocessorUrl)

    @Autowired
    private lateinit var testSuitesService: TestSuitesService

    /**
     * Save new test suites into DB
     *
     * @param testSuiteDtos
     * @return mono list of *all* TestSuite
     */
    @PostMapping("/saveTestSuites")
    fun saveTestSuite(@RequestBody testSuiteDtos: List<TestSuiteDto>): Mono<List<TestSuite>> =
            Mono.just(testSuitesService.saveTestSuite(testSuiteDtos))

    /**
     * @return response with list of test suite dtos
     */
    @GetMapping("/allStandardTestSuites")
    fun getAllStandardTestSuites(): ResponseListTestSuites =
            ResponseEntity.status(HttpStatus.OK).body(testSuitesService.getStandardTestSuites())

    /**
     * @param name name of the test suite
     * @return response with list of test suite with specific name
     */
    @GetMapping("/standardTestSuitesWithName")
    fun getAllStandardTestSuitesWithSpecificName(@RequestParam name: String) =
            ResponseEntity.status(HttpStatus.OK).body(testSuitesService.findStandardTestSuitesByName(name))

    /**
     * @param id id of the test suite
     * @return response with test suite with provided id
     */
    @GetMapping("/testSuiteWithId")
    fun getTestSuiteById(@RequestParam id: Long) =
            ResponseEntity.status(HttpStatus.OK).body(testSuitesService.findTestSuitesById(id))

    /**
     * @return response entity
     */
    @PostMapping("/updateStandardTestSuites")
    fun updateStandardTestSuites() = Mono.fromCallable {
        scheduler.triggerJob(
            JobKey.jobKey(StandardSuitesUpdateScheduler.jobName)
        )
    }

    /**
     * @param testSuiteDtos suites, which need to be marked as obsolete
     * @return response entity
     */
    @PostMapping("/markObsoleteTestSuites")
    @Transactional
    fun markObsoleteTestSuites(@RequestBody testSuiteDtos: List<TestSuiteDto>) =
            ResponseEntity.status(HttpStatus.OK).body(testSuitesService.markObsoleteTestSuites(testSuiteDtos))

    /**
     * @param testSuiteDtos suites, which need to be deleted
     * @return response entity
     */
    @PostMapping("/deleteTestSuite")
    @Transactional
    fun deleteTestSuite(@RequestBody testSuiteDtos: List<TestSuiteDto>) =
            ResponseEntity.status(HttpStatus.OK).body(testSuitesService.deleteTestSuiteDto(testSuiteDtos))
}
