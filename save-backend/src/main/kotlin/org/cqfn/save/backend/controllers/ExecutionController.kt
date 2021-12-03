package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.backend.service.AgentService
import org.cqfn.save.backend.service.AgentStatusService
import org.cqfn.save.backend.service.ExecutionService
import org.cqfn.save.backend.service.GitService
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.backend.service.TestExecutionService
import org.cqfn.save.backend.service.TestSuitesService
import org.cqfn.save.domain.toSdk
import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.execution.ExecutionInitializationDto
import org.cqfn.save.execution.ExecutionType
import org.cqfn.save.execution.ExecutionUpdateDto
import org.slf4j.LoggerFactory

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

typealias ExecutionDtoListResponse = ResponseEntity<List<ExecutionDto>>

/**
 * Controller that accepts executions
 */
@RestController
@Suppress("LongParameterList")
class ExecutionController(private val executionService: ExecutionService,
                          private val gitService: GitService,
                          private val testSuitesService: TestSuitesService,
                          private val projectService: ProjectService,
                          private val testExecutionService: TestExecutionService,
                          private val agentService: AgentService,
                          private val agentStatusService: AgentStatusService,
                          config: ConfigProperties,
) {
    private val log = LoggerFactory.getLogger(ExecutionController::class.java)
    private val preprocessorWebClient = WebClient.create(config.preprocessorUrl)

    /**
     * @param execution
     * @return id of created [Execution]
     */
    @PostMapping("/internal/createExecution")
    fun createExecution(@RequestBody execution: Execution): Long = executionService.saveExecution(execution)

    /**
     * @param executionUpdateDto
     */
    @PostMapping("/internal/updateExecutionByDto")
    fun updateExecution(@RequestBody executionUpdateDto: ExecutionUpdateDto) {
        executionService.updateExecution(executionUpdateDto)
    }

    /**
     * @param execution
     */
    @PostMapping("/internal/updateExecution")
    fun updateExecution(@RequestBody execution: Execution) {
        executionService.updateExecution(execution)
    }

    /**
     * Get execution by id
     *
     * @param id id of execution
     * @return execution if it has been found
     */
    @GetMapping(path = ["/api/execution", "/internal/execution"])
    @Transactional(readOnly = true)
    fun getExecution(@RequestParam id: Long): Execution = executionService.findExecution(id).orElseThrow {
        ResponseStatusException(HttpStatus.NOT_FOUND, "Execution with id=$id is not found")
    }

    /**
     * @param executionInitializationDto
     * @return execution
     */
    @PostMapping("/updateNewExecution")
    fun updateNewExecution(@RequestBody executionInitializationDto: ExecutionInitializationDto): ResponseEntity<Execution> =
            executionService.updateNewExecution(executionInitializationDto)?.let {
                ResponseEntity.status(HttpStatus.OK).body(it)
            } ?: ResponseEntity.status(HttpStatus.NOT_FOUND).build()

    /**
     * @param executionId
     * @return execution dto
     */
    @GetMapping("/executionDto")
    fun getExecutionDto(@RequestParam executionId: Long): ResponseEntity<ExecutionDto> =
            executionService.getExecutionDto(executionId)?.let {
                ResponseEntity.status(HttpStatus.OK).body(it)
            } ?: ResponseEntity.status(HttpStatus.NOT_FOUND).build()

    /**
     * @param name
     * @param owner
     * @return list of execution dtos
     */
    @GetMapping("/executionDtoList")
    fun getExecutionByProject(@RequestParam name: String, @RequestParam owner: String): ExecutionDtoListResponse =
            ResponseEntity
                .status(HttpStatus.OK)
                .body(executionService.getExecutionDtoByNameAndOwner(name, owner).reversed())

    /**
     * Get latest (by start time an) execution by project name and project owner
     *
     * @param name project name
     * @param owner project owner
     * @return Execution
     * @throws ResponseStatusException if execution is not found
     */
    @GetMapping("/latestExecution")
    fun getLatestExecutionForProject(@RequestParam name: String, @RequestParam owner: String): Mono<ExecutionDto> =
            Mono.fromCallable { executionService.getLatestExecutionByProjectNameAndProjectOwner(name, owner) }
                .map { execOpt ->
                    execOpt.map { it.toDto() }.orElseThrow {
                        ResponseStatusException(HttpStatus.NOT_FOUND, "Execution not found for project (name=$name, owner=$owner)")
                    }
                }

    /**
     * Delete all executions by project name and project owner
     *
     * @param name name of project
     * @param owner owner of project
     * @return ResponseEntity
     * @throws ResponseStatusException
     */
    @PostMapping("/execution/deleteAll")
    @Suppress("UnsafeCallOnNullableType")
    fun deleteExecutionForProject(@RequestParam name: String, @RequestParam owner: String): ResponseEntity<String> {
        try {
            requireNotNull(projectService.findByNameAndOwner(name, owner)).id!!.let {
                testExecutionService.deleteTestExecutionWithProjectId(it)
                agentStatusService.deleteAgentStatusWithProjectId(it)
                agentService.deleteAgentWithProjectId(it)
                executionService.deleteExecutionByProjectNameAndProjectOwner(name, owner)
            }
        } catch (e: IllegalArgumentException) {
            log.warn("Could not find the project with name: $name and owner: $owner or related objects", e)
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to delete executions for the following reason: ${e.message}")
        }
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    /**
     * @param executionIds list of ids
     * @return ResponseEntity
     * @throws ResponseStatusException
     */
    @PostMapping("/execution/delete")
    fun deleteExecutionsByExecutionIds(@RequestParam executionIds: List<Long>): ResponseEntity<String>? {
        try {
            testExecutionService.deleteTestExecutionByExecutionIds(executionIds)
            agentStatusService.deleteAgentStatusWithExecutionIds(executionIds)
            agentService.deleteAgentByExecutionIds(executionIds)
            executionService.deleteExecutionByIds(executionIds)
        } catch (e: IllegalArgumentException) {
            log.warn("Could not find the following executions: $executionIds or related objects", e)
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to delete executions for the following reason: ${e.message}")
        }
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    /**
     * Accepts a request to rerun an existing execution
     *
     * @param id id of an existing execution
     * @return bodiless response
     */
    @PostMapping("/rerunExecution")
    @Transactional
    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
    fun rerunExecution(@RequestParam id: Long): Mono<String> {
        val execution = executionService.findExecution(id).orElseThrow {
            IllegalArgumentException("Can't rerun execution $id, because it does not exist")
        }
        val executionType = execution.type
        val git = requireNotNull(gitService.getRepositoryDtoByProject(execution.project)) {
            "Can't rerun execution $id, project ${execution.project.name} has no associated git address"
        }
        val testRootPath = if (executionType == ExecutionType.GIT) {
            execution.getTestRootPathByTestSuites()
                .distinct()
                .single()
        } else {
            // for standard suites there is no need for a testRootPath
            "N/A"
        }

        executionService.resetMetrics(execution)
        val executionRequest = ExecutionRequest(
            project = execution.project,
            gitDto = git.copy(hash = execution.version),
            testRootPath = testRootPath,
            sdk = execution.sdk.toSdk(),
            executionId = execution.id
        )
        return preprocessorWebClient.post()
            .uri("/rerunExecution?executionType=$executionType")
            .bodyValue(executionRequest)
            .retrieve()
            .bodyToMono()
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun Execution.getTestRootPathByTestSuites(): List<String> = this.testSuiteIds?.split(", ")?.map { testSuiteId ->
        testSuitesService.findTestSuiteById(testSuiteId.toLong()).orElseThrow {
            log.error("Can't find test suite with id=$testSuiteId for executionId=$id")
            NoSuchElementException()
        }
    }!!
        .map {
            it.testRootPath
        }

    /**
     * @param execution
     * @return the list of the testRootPaths for current execution; size of the list could be >1 only in standard mode
     */
    @PostMapping("/findTestRootPathForExecutionByTestSuites")
    fun findTestRootPathByTestSuites(@RequestBody execution: Execution): List<String> = execution.getTestRootPathByTestSuites()
}
