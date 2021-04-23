package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.TestSuitesService
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.testsuite.TestSuiteDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Controller for test suites
 */
@RestController
class TestSuitesController {
    @Autowired
    private lateinit var testSuitesService: TestSuitesService

    /**
     * @param testSuiteDtos
     *
     * @return mono list of TestSuite
     */
    @PostMapping("/saveTestSuites")
    fun saveTestSuite(@RequestBody testSuiteDtos: List<TestSuiteDto>): Mono<List<TestSuite>> =
        Mono.just(testSuitesService.saveTestSuite(testSuiteDtos))
}
