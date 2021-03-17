package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.TestSuitesService
import org.cqfn.save.entities.TestSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for test suites
 */
@RestController
class TestSuitesController {
    @Autowired
    private lateinit var testSuitesService: TestSuitesService

    /**
     * @param testSuite
     */
    @PostMapping("/saveTestSuite")
    fun saveTestSuite(@RequestBody testSuite: TestSuite) {
        testSuitesService.saveTestSuite(testSuite)
    }
}
