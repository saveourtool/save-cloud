package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.EmptyResponse
import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.backend.service.ExecutionService
import org.cqfn.save.backend.service.GitService
import org.cqfn.save.domain.toSdk
import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.GitDto
import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.execution.ExecutionInitializationDto
import org.cqfn.save.execution.ExecutionUpdateDto

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

typealias ExecutionDtoListResponse = ResponseEntity<List<ExecutionDto>>

/**
 * Controller that accepts executions
 */
@RestController
class ExecutionController(private val executionService: ExecutionService,
                          private val gitService: GitService,
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
    @PostMapping("/updateExecution")
    fun updateExecution(@RequestBody executionUpdateDto: ExecutionUpdateDto) {
        executionService.updateExecution(executionUpdateDto)
    }

    /**
     * Get execution by id
     *
     * @param id id of execution
     * @return execution if it has been found
     */
    @GetMapping("/execution")
    @Transactional(readOnly = true)
    fun getExecution(@RequestParam id: Long) = executionService.getExecution(id).also {
        println(it.project.name)
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
    @Suppress("UnsafeCallOnNullableType")
    fun rerunExecution(@RequestParam id: Long): Mono<EmptyResponse> {
        val execution = requireNotNull(executionService.getExecution(id)) {
            "Can't rerun execution $id, because it does not exist"
        }
        val git = requireNotNull(gitService.getRepositoryDtoByProject(execution.project)) {
            "Can't rerun execution $id, project ${execution.project.name} has no associated git address"
        }
        val executionRequest = ExecutionRequest(
            project = execution.project,
            gitDto = GitDto(git.url, hash = execution.version),
            sdk = execution.sdk.toSdk(),
            executionId = execution.id
        )
        return preprocessorWebClient.post()
            .uri("/rerunExecution")
            .bodyValue(executionRequest)
            .retrieve()
            .toBodilessEntity()
    }
}
