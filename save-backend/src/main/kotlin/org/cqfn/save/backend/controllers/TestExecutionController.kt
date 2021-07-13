package org.cqfn.save.backend.controllers

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.backend.service.TestExecutionService
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Controller to work with test execution
 *
 * @param testExecutionService service for test execution
 */
@RestController
@Transactional
class TestExecutionController(private val testExecutionService: TestExecutionService) {
    /**
     * Returns a page of [TestExecution]s with [executionId]
     *
     * @param executionId an ID of Execution to group TestExecutions
     * @param page a zero-based index of page of data
     * @param size size of page
     * @return a list of [TestExecutionDto]s
     */
    @GetMapping("/testExecutions")
    fun getTestExecutions(@RequestParam executionId: Long, @RequestParam page: Int, @RequestParam size: Int): List<TestExecutionDto> {
        println("Request to get test executions on page $page with size $size for execution $executionId")
        return testExecutionService.getTestExecutions(executionId, page, size)
            .map { it.toDto() }
    }

    /**
     * Returns number of TestExecutions with this [executionId]
     *
     * @param executionId an ID of Execution to group TestExecutions
     */
    @GetMapping("/testExecutionsCount")
    fun getTestExecutionsCount(@RequestParam executionId: Long) =
            testExecutionService.getTestExecutionsCount(executionId)

    /**
     * @param testExecutionsDto
     * @return response
     */
    @PostMapping(value = ["/saveTestResult"])
    fun saveTestResult(@RequestBody testExecutionsDto: List<TestExecutionDto>) = try {
        val lostTests = testExecutionService.saveTestResult(testExecutionsDto)
        if (lostTests.isEmpty()) {
            ResponseEntity.status(HttpStatus.OK).body("Saved")
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Some ids don't exist")
        }
    } catch (exception: DataAccessException) {
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error to save")
    }
}
