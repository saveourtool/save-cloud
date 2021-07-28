package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.TestSuitesService
import org.cqfn.save.entities.GitDto
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.testsuite.TestSuiteDto
import org.cqfn.save.testsuite.TestSuiteType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

/**
 * Controller for test suites
 */
@RestController
class TestSuitesController {
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

    @GetMapping("/allStandardTestSuites")
    fun getAllStandardTestSuites(): ResponseEntity<List<TestSuiteDto>> =
        ResponseEntity.status(HttpStatus.OK).body(testSuitesService.getStandardTestSuites())
}
