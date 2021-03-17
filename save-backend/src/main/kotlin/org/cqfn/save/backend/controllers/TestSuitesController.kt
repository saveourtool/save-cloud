package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.TestSuitesService
import org.cqfn.save.entities.TestSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TestSuitesController {
    @Autowired
    private lateinit var testSuitesService: TestSuitesService

    @PostMapping("/saveTestSuite")
    fun saveTestSuite(testSuite: TestSuite) {
        testSuitesService.saveTestSuite(testSuite)
    }
}