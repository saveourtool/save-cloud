package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.backend.security.Permission
import org.cqfn.save.backend.security.ProjectPermissionEvaluator
import org.cqfn.save.backend.service.AgentService
import org.cqfn.save.backend.service.AgentStatusService
import org.cqfn.save.backend.service.ExecutionService
import org.cqfn.save.backend.service.GitService
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.backend.service.TestExecutionService
import org.cqfn.save.backend.service.TestSuitesService
import org.cqfn.save.backend.utils.filterAndInvoke
import org.cqfn.save.backend.utils.filterWhenAndInvoke
import org.cqfn.save.backend.utils.username
import org.cqfn.save.backend.utils.justOrNotFound
import org.cqfn.save.core.utils.runIf
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
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.util.Optional

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
                          private val projectPermissionEvaluator: ProjectPermissionEvaluator,
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
    @Suppress("UnsafeCallOnNullableType")
    fun getExecution(@RequestParam id: Long, authentication: Authentication?): Mono<Execution> {
        return justOrNotFound(executionService.findExecution(id), "Execution with id=$id is not found")
            .runIf({ authentication != null }) {
                filterWhen { checkPermissions(authentication!!, it, Permission.READ) }
            }
    }

    /**
     * @param executionInitializationDto
     * @return execution
     */
    @PostMapping("/internal/updateNewExecution")
    fun updateNewExecution(@RequestBody executionInitializationDto: ExecutionInitializationDto): ResponseEntity<Execution> =
            executionService.updateNewExecution(executionInitializationDto)?.let {
                ResponseEntity.status(HttpStatus.OK).body(it)
            } ?: ResponseEntity.status(HttpStatus.NOT_FOUND).build()

    /**
     * @param executionId
     * @return execution dto
     */
    @GetMapping("/api/executionDto")
    fun getExecutionDto(@RequestParam executionId: Long, authentication: Authentication): Mono<ExecutionDto> =
        justOrNotFound(executionService.findExecution(executionId))
            .filterWhen { checkPermissions(authentication, it, Permission.READ) }
            .map { it.toDto() }

    /**
     * @param name
     * @param owner
     * @return list of execution dtos
     */
    @GetMapping("/api/executionDtoList")
    fun getExecutionByProject(authentication: Authentication, @RequestParam name: String, @RequestParam owner: String): Mono<List<ExecutionDto>> =
        projectService.findWithPermissionByNameAndOwner(authentication, name, owner, Permission.READ).map {
            executionService.getExecutionDtoByNameAndOwner(name, owner).reversed()
        }

    /**
     * Get latest (by start time an) execution by project name and project owner
     *
     * @param name project name
     * @param owner project owner
     * @return Execution
     * @throws ResponseStatusException if execution is not found
     */
    @GetMapping("/api/latestExecution")
    fun getLatestExecutionForProject(@RequestParam name: String, @RequestParam owner: String, authentication: Authentication): Mono<ExecutionDto> =
            justOrNotFound(
                executionService.getLatestExecutionByProjectNameAndProjectOwner(name, owner),
                "Execution not found for project (name=$name, owner=$owner)"
            )
                .filterWhen { checkPermissions(authentication, it, Permission.READ) }
                .map { it.toDto() }

    /**
     * Delete all executions by project name and project owner
     *
     * @param name name of project
     * @param owner owner of project
     * @return ResponseEntity
     * @throws ResponseStatusException
     */
    @PostMapping("/api/execution/deleteAll")
    @Suppress("UnsafeCallOnNullableType")
    fun deleteExecutionForProject(
        @RequestParam name: String,
        @RequestParam owner: String,
        authentication: Authentication,
    ): Mono<ResponseEntity<*>> {
        return projectService.findWithPermissionByNameAndOwner(
            authentication,
            name,
            owner,
            Permission.DELETE,
            messageIfNotFound = "Could not find the project with name: $name and owner: $owner or related objects",
        )
            .mapNotNull { it.id!! }
            .map { id ->
                testExecutionService.deleteTestExecutionWithProjectId(id)
                agentStatusService.deleteAgentStatusWithProjectId(id)
                agentService.deleteAgentWithProjectId(id)
                executionService.deleteExecutionByProjectNameAndProjectOwner(name, owner)
                ResponseEntity.ok().build<String>()
            }
    }

    /**
     * FixMe: do we need to add preconditions that only executions from a single project can be deleted in a single query?
     *
     * @param executionIds list of ids
     * @return ResponseEntity
     * @throws ResponseStatusException
     */
    @PostMapping("/api/execution/delete")
    fun deleteExecutionsByExecutionIds(@RequestParam executionIds: List<Long>, authentication: Authentication): Mono<ResponseEntity<*>> {
       return Flux.fromIterable(executionIds)
        .map { it to executionService.findExecution(it) }
            .filterAndInvoke({ (id, _) -> log.warn("Cannot delete execution id=$id because it's missing in the DB") }) { (_, execution) ->
                execution.isPresent
            }
            .map { (_, execution) -> execution.get() }
            .groupBy { it.project }
            .flatMap { groupedFlux ->
                val project = groupedFlux.key()
                groupedFlux.filterWhenAndInvoke({ log.warn("Cannot delete execution id=${it.id}, because operation is not allowed on project id=${project.id}") }) { execution ->
                    checkPermissions(authentication, execution, Permission.DELETE)
                }
            }
            .map { it.id!! }
            .collectList()
            .map { filteredExecutionIds ->
                testExecutionService.deleteTestExecutionByExecutionIds(filteredExecutionIds)
                agentStatusService.deleteAgentStatusWithExecutionIds(filteredExecutionIds)
                agentService.deleteAgentByExecutionIds(filteredExecutionIds)
                executionService.deleteExecutionByIds(filteredExecutionIds)
                ResponseEntity.ok().build<Void>()
            }
    }

    /**
     * Accepts a request to rerun an existing execution
     *
     * @param id id of an existing execution
     * @param authentication [Authentication] representing an authenticated request
     * @return bodiless response
     */
    @PostMapping("/api/rerunExecution")
    @Transactional
    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
    fun rerunExecution(@RequestParam id: Long, authentication: Authentication): Mono<String> {
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
        executionService.updateExecutionWithUser(execution, authentication.username())
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
    @PostMapping("/internal/findTestRootPathForExecutionByTestSuites")
    fun findTestRootPathByTestSuites(@RequestBody execution: Execution): List<String> = execution.getTestRootPathByTestSuites()

    private fun checkPermissions(authentication: Authentication, execution: Execution, permission: Permission): Mono<Boolean> =
        with (projectPermissionEvaluator) {
            Mono.justOrEmpty(execution.project)
                .filterByPermission(authentication, permission, HttpStatus.FORBIDDEN)
                .map { true }
                .defaultIfEmpty(false)
        }
}
