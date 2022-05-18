package org.cqfn.save.backend.controllers

import org.cqfn.save.agent.AgentState
import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.agent.TestSuiteExecutionStatisticDto
import org.cqfn.save.backend.configs.ApiSwaggerSupport
import org.cqfn.save.backend.configs.RequiresAuthorizationSourceHeader
import org.cqfn.save.backend.repository.TestDataFilesystemRepository
import org.cqfn.save.backend.security.ProjectPermissionEvaluator
import org.cqfn.save.backend.service.ExecutionService
import org.cqfn.save.backend.service.TestExecutionService
import org.cqfn.save.backend.utils.justOrNotFound
import org.cqfn.save.core.utils.runIf
import org.cqfn.save.domain.TestResultLocation
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.permission.Permission
import org.cqfn.save.test.TestDto
import org.cqfn.save.v1

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.math.BigInteger

import kotlin.io.path.exists

/**
 * Controller to work with test execution
 *
 * @param testExecutionService service for test execution
 */
@ApiSwaggerSupport
@Tags(Tag(name = "test-executions"))
@RestController
@Transactional
class TestExecutionController(private val testExecutionService: TestExecutionService,
                              private val executionService: ExecutionService,
                              private val projectPermissionEvaluator: ProjectPermissionEvaluator,
                              private val testDataFilesystemRepository: TestDataFilesystemRepository
) {
    /**
     * Returns a page of [TestExecutionDto]s with [executionId]
     *
     * @param executionId an ID of Execution to group TestExecutions
     * @param page a zero-based index of page of data
     * @param size size of page
     * @param status
     * @param testSuite
     * @param authentication
     * @param checkDebugInfo if true, response will contain information about whether debug info data is available for this test execution
     * @return a list of [TestExecutionDto]s
     */
    @GetMapping(path = ["/api/$v1/testExecutions"])
    @RequiresAuthorizationSourceHeader
    @Suppress("LongParameterList", "TOO_MANY_PARAMETERS", "TYPE_ALIAS")
    fun getTestExecutions(
        @RequestParam executionId: Long,
        @RequestParam page: Int,
        @RequestParam size: Int,
        @RequestParam(required = false) status: TestResultStatus?,
        @RequestParam(required = false) testSuite: String?,
        @RequestParam(required = false, defaultValue = "false") checkDebugInfo: Boolean,
        authentication: Authentication,
    ): Flux<TestExecutionDto> = justOrNotFound(executionService.findExecution(executionId)).filterWhen {
        projectPermissionEvaluator.checkPermissions(authentication, it, Permission.READ)
    }
        .flatMapIterable {
            log.debug("Request to get test executions on page $page with size $size for execution $executionId")
            testExecutionService.getTestExecutions(executionId, page, size, status, testSuite)
        }
        .map { it.toDto() }
        .runIf({ checkDebugInfo }) {
            map { testExecutionDto ->
                val debugInfoFile = testDataFilesystemRepository.getLocation(
                    executionId,
                    testExecutionDto
                )
                testExecutionDto.copy(
                    hasDebugInfo = debugInfoFile.exists()
                )
            }
        }

    /**
     * @param executionId an ID of Execution to group TestExecutions
     * @param status of test
     * @param page a zero-based index of page of data
     * @param size size of page
     * @param authentication
     * @return a list of [TestExecutionDto]s
     */
    @GetMapping(path = ["/api/$v1/testLatestExecutions"])
    @RequiresAuthorizationSourceHeader
    @Suppress("TYPE_ALIAS", "MagicNumber")
    fun getTestExecutionsByStatus(
        @RequestParam executionId: Long,
        @RequestParam status: TestResultStatus,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
        authentication: Authentication,
    ): Mono<List<TestSuiteExecutionStatisticDto>> =
            justOrNotFound(executionService.findExecution(executionId)).filterWhen {
                projectPermissionEvaluator.checkPermissions(authentication, it, Permission.READ)
            }.mapNotNull {
                if (page == null || size == null) {
                    testExecutionService.getTestExecutions(executionId).groupBy { it.test.testSuite.name }.map { (testSuiteName, testExecutions) ->
                        TestSuiteExecutionStatisticDto(testSuiteName, testExecutions.count(), testExecutions.count { it.status == status }, status)
                    }
                } else {
                    testExecutionService.getByExecutionIdGroupByTestSuite(executionId, status, page, size)?.map {
                        TestSuiteExecutionStatisticDto(it[0] as String, (it[1] as BigInteger).toInt(), (it[2] as BigInteger).toInt(), TestResultStatus.valueOf(it[3] as String))
                    }
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
     * @param authentication
     * @return TestExecution
     */
    @PostMapping(path = ["/api/$v1/testExecutions"])
    @RequiresAuthorizationSourceHeader
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
     * @param authentication
     */
    @GetMapping(path = ["/api/$v1/testExecution/count"])
    @RequiresAuthorizationSourceHeader
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
     * @param status
     * @param agentIds the list of agents, for which, according the [status] test executions should be updated
     * @throws ResponseStatusException
     */
    @PostMapping(value = ["/internal/testExecution/setStatusByAgentIds"])
    fun setStatusByAgentIds(
        @RequestParam("status") status: String,
        @RequestBody agentIds: Collection<String>
    ) {
        when (status) {
            AgentState.CRASHED.name -> testExecutionService.markTestExecutionsOfAgentsAsFailed(agentIds)
            AgentState.FINISHED.name -> testExecutionService.markTestExecutionsOfAgentsAsFailed(agentIds) {
                it.status == TestResultStatus.READY_FOR_TESTING
            }
            else -> throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "For now only CRASHED and FINISHED statuses are supported"
            )
        }
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
