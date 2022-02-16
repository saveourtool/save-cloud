package org.cqfn.save.backend.controllers

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.backend.security.Permission
import org.cqfn.save.backend.security.ProjectPermissionEvaluator
import org.cqfn.save.backend.service.ExecutionService
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.backend.service.TestExecutionService
import org.cqfn.save.backend.utils.justOrNotFound
import org.cqfn.save.domain.TestResultLocation
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.entities.Project
import org.cqfn.save.test.TestDto
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

/**
 * Controller to work with test execution
 *
 * @param testExecutionService service for test execution
 */
@RestController
@Transactional
class TestExecutionController(private val testExecutionService: TestExecutionService,
                              private val executionService: ExecutionService,
                              private val projectPermissionEvaluator: ProjectPermissionEvaluator,
) {
    /**
     * Returns a page of [TestExecution]s with [executionId]
     *
     * @param executionId an ID of Execution to group TestExecutions
     * @param page a zero-based index of page of data
     * @param size size of page
     * @param status
     * @param testSuite
     * @return a list of [TestExecutionDto]s
     */
    @GetMapping("/api/testExecutions")
    fun getTestExecutions(
        @RequestParam executionId: Long,
        @RequestParam page: Int,
        @RequestParam size: Int,
        @RequestParam(required = false) status: TestResultStatus?,
        @RequestParam(required = false) testSuite: String?,
        authentication: Authentication,
    ): Mono<List<TestExecutionDto>> {
        return justOrNotFound(executionService.findExecution(executionId)).filterWhen {
            projectPermissionEvaluator.checkPermissions(authentication, it, Permission.READ)
        }
        .map {
            log.debug("Request to get test executions on page $page with size $size for execution $executionId")
            testExecutionService.getTestExecutions(executionId, page, size, status, testSuite)
                .map { it.toDto() }
        }
    }

    /**
     * @param agentContainerId id of agent's container
     * @param status status for test executions
     * @return a list of test executions
     */
    @GetMapping("/internal/testExecutions/agent/{agentId}/{status}")
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
    @PostMapping("/api/testExecutions")
    fun getTestExecutionByLocation(@RequestParam executionId: Long,
                                   @RequestBody testResultLocation: TestResultLocation,
                                   authentication: Authentication,
    ): Mono<TestExecutionDto> = justOrNotFound(executionService.findExecution(executionId)).filterWhen {
            projectPermissionEvaluator.checkPermissions(authentication, it, Permission.READ)
        }
        .map {
            testExecutionService.getTestExecution(executionId, testResultLocation)
                .map { it.toDto() }
                .orElseThrow {
                    ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Test execution not found for executionId=$executionId and $testResultLocation"
                    )
                }
        }

    /**
     * Returns number of TestExecutions with this [executionId]
     *
     * @param executionId an ID of Execution to group TestExecutions
     * @param status
     * @param testSuite
     */
    @GetMapping("/api/testExecution/count")
    fun getTestExecutionsCount(
        @RequestParam executionId: Long,
        @RequestParam(required = false) status: TestResultStatus?,
        @RequestParam(required = false) testSuite: String?,
        authentication: Authentication,
    ) =
        justOrNotFound(executionService.findExecution(executionId)).filterWhen {
            projectPermissionEvaluator.checkPermissions(authentication, it, Permission.READ)
        }.map {
            testExecutionService.getTestExecutionsCount(executionId, status, testSuite)
        }

    /**
     * @param agentContainerId id of an agent
     * @param testDtos test that will be executed by [agentContainerId] agent
     */
    @PostMapping(value = ["/internal/testExecution/assignAgent"])
    fun assignAgentByTest(@RequestParam agentContainerId: String, @RequestBody testDtos: List<TestDto>) {
        testExecutionService.assignAgentByTest(agentContainerId, testDtos)
    }

    /**
     * @param testExecutionsDto
     * @return response
     */
    @PostMapping(value = ["/internal/saveTestResult"])
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
