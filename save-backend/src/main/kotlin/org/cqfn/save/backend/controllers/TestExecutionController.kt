package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.TestExecutionService
import org.cqfn.save.entities.TestExecution
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * Controller to work with test execution
 *
 * @param testExecutionService service for test execution
 */
@RestController
class TestExecutionController(private val testExecutionService: TestExecutionService) {
    /**
     * @param testExecutions list of test executions
     * @return
     */
    @PostMapping(value = ["/saveTestResult"])
    fun saveTestResult(@RequestBody testExecutions: List<TestExecution>): ResponseEntity<String> {
        testExecutionService.saveTestResult(testExecutions)
        return ResponseEntity.ok().body("Save")
    }
}
