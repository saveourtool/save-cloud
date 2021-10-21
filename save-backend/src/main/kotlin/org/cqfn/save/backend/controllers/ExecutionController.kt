package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.backend.service.ExecutionService
import org.cqfn.save.backend.service.GitService
import org.cqfn.save.backend.service.TestSuitesService
import org.cqfn.save.domain.toSdk
import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.execution.ExecutionInitializationDto
import org.cqfn.save.execution.ExecutionType
import org.cqfn.save.execution.ExecutionUpdateDto
import org.cqfn.save.testsuite.TestSuiteType

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
class ExecutionController(private val executionService: ExecutionService,
                          private val gitService: GitService,
                          private val testSuitesService: TestSuitesService,
                          config: ConfigProperties,
) {
    private val preprocessorWebClient = WebClient.create(config.preprocessorUrl)

    /**
     * @param execution
     * @return id of created [Execution]
     */
    @PostMapping("/createExecution")
    fun createExecution(@RequestBody execution: Execution): Long = executionService.saveExecution(execution)

    /**
     * @param executionUpdateDto
     */
    @PostMapping("/updateExecutionByDto")
    fun updateExecution(@RequestBody executionUpdateDto: ExecutionUpdateDto) {
        executionService.updateExecution(executionUpdateDto)
    }

    /**
     * @param execution
     */
    @PostMapping("/updateExecution")
    fun updateExecution(@RequestBody execution: Execution) {
        executionService.updateExecution(execution)
    }

    /**
     * Get execution by id
     *
     * @param id id of execution
     * @return execution if it has been found
     */
    @GetMapping("/execution")
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
        val propertiesRelativePath = if (executionType == ExecutionType.GIT) {
            execution.testSuiteIds?.let {
                require(it == "ALL") { "Only executions with \"ALL\" tests suites from a GIT project are supported now" }
                testSuitesService.findTestSuitesByProject(execution.project)
            }!!
                .filter {
                    it.type == TestSuiteType.PROJECT
                }
                .map {
                    it.propertiesRelativePath
                }
                .distinct()
                .single()
        } else {
            "save.properties"
        }
        execution.apply {
            runningTests = 0
            passedTests = 0
            failedTests = 0
            skippedTests = 0
        }
        executionService.saveExecution(execution)
        val executionRequest = ExecutionRequest(
            project = execution.project,
            gitDto = git.copy(hash = execution.version),
            propertiesRelativePath = propertiesRelativePath,
            sdk = execution.sdk.toSdk(),
            executionId = execution.id
        )
        return preprocessorWebClient.post()
            .uri("/rerunExecution?executionType=$executionType")
            .bodyValue(executionRequest)
            .retrieve()
            .bodyToMono()
    }
}
