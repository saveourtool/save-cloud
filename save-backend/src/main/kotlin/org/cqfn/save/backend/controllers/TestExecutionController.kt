package org.cqfn.save.backend.controllers

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.backend.service.TestExecutionService
import org.cqfn.save.domain.TestResultLocation
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.test.TestDto
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

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
        log.debug("Request to get test executions on page $page with size $size for execution $executionId")
        return testExecutionService.getTestExecutions(executionId, page, size)
            .map { it.toDto() }
    }

    /**
     * @param agentContainerId id of agent's container
     * @param status status for test executions
     * @return a list of test executions
     */
    @GetMapping("/testExecutions/agent/{agentId}/{status}")
    fun getTestExecutionsForAgentWithStatus(@PathVariable("agentId") agentContainerId: String,
                                            @PathVariable status: TestResultStatus
    ) = testExecutionService.getTestExecutions(agentContainerId, status)
        .map { it.toDto() }

    /**
     * Finds TestExecution by test location, returns 404 if not found
     *
     * @param executionId under this executionId test has been executed
     * @param testResultLocation location of the test
     * @return TestExecution
     */
    @PostMapping("/testExecutions")
    fun getTestExecutionByLocation(@RequestParam executionId: Long,
                                   @RequestBody testResultLocation: TestResultLocation,
    ): TestExecutionDto = testExecutionService.getTestExecution(executionId, testResultLocation)
        .map { it.toDto() }
        .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Test execution not found for executionId=$executionId and $testResultLocation") }

    /**
     * Returns number of TestExecutions with this [executionId]
     *
     * @param executionId an ID of Execution to group TestExecutions
     */
    @Suppress("KDOC_WITHOUT_RETURN_TAG")  // https://github.com/cqfn/diKTat/issues/965
    @GetMapping("/testExecutionsCount")
    fun getTestExecutionsCount(@RequestParam executionId: Long) =
            testExecutionService.getTestExecutionsCount(executionId)

    /**
     * @param agentContainerId id of an agent
     * @param testDtos test that will be executed by [agentContainerId] agent
     */
    @PostMapping(value = ["/testExecution/assignAgent"])
    fun assignAgentByTest(@RequestParam agentContainerId: String, @RequestBody testDtos: List<TestDto>) {
        testExecutionService.assignAgentByTest(agentContainerId, testDtos)
    }

    /**
     * @param testExecutionsDto
     * @return response
     */
    @PostMapping(value = ["/saveTestResult"])
    fun saveTestResult(@RequestBody testExecutionsDto: List<TestExecutionDto>) = try {
        if (testExecutionService.saveTestResult(testExecutionsDto).isEmpty()) {
            ResponseEntity.status(HttpStatus.OK).body("Saved")
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Some ids don't exist or cannot be updated")
        }
    } catch (exception: DataAccessException) {
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error to save")
    }

    companion object {
        private val log = LoggerFactory.getLogger(TestExecutionController::class.java)
    }
}
