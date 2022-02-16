package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.backend.security.Permission
import org.cqfn.save.backend.security.ProjectPermissionEvaluator
import org.cqfn.save.backend.service.AgentService
import org.cqfn.save.backend.service.AgentStatusService
import org.cqfn.save.backend.service.ExecutionService
import org.cqfn.save.backend.service.GitService
import org.cqfn.save.backend.service.OrganizationService
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.backend.service.TestExecutionService
import org.cqfn.save.backend.service.TestSuitesService
import org.cqfn.save.backend.utils.filterAndInvoke
import org.cqfn.save.backend.utils.filterWhenAndInvoke
import org.cqfn.save.backend.utils.justOrNotFound
import org.cqfn.save.backend.utils.username
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
                          private val organizationService: OrganizationService,
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
     * @param authentication
     * @return execution if it has been found
     */
    @GetMapping(path = ["/api/execution", "/internal/execution"])
    @Transactional(readOnly = true)
    @Suppress("UnsafeCallOnNullableType")
    fun getExecution(
        @RequestParam id: Long,
        authentication: Authentication?
    ): Mono<Execution> = justOrNotFound(executionService.findExecution(id), "Execution with id=$id is not found")
        .runIf({ authentication != null }) {
            filterWhen { execution -> projectPermissionEvaluator.checkPermissions(authentication!!, execution, Permission.READ) }
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
     * @param authentication
     * @return execution dto
     */
    @GetMapping("/api/executionDto")
    fun getExecutionDto(@RequestParam executionId: Long, authentication: Authentication): Mono<ExecutionDto> =
            justOrNotFound(executionService.findExecution(executionId))
                .filterWhen { execution -> projectPermissionEvaluator.checkPermissions(authentication, execution, Permission.READ) }
                .map { it.toDto() }

    /**
     * @param name
     * @param organizationId
     * @param authentication
     * @return list of execution dtos
     */
    @GetMapping("/api/executionDtoList")
    fun getExecutionByProject(authentication: Authentication, @RequestParam name: String, @RequestParam organizationId: Long): Mono<List<ExecutionDto>> {
        val organization = organizationService.getOrganizationById(organizationId)
        return projectService.findWithPermissionByNameAndOrganization(authentication, name, organization, Permission.READ).map {
            executionService.getExecutionDtoByNameAndOrganization(name, organization).reversed()
        }
    }

    /**
     * Get latest (by start time an) execution by project name and project owner
     *
     * @param name project name
     * @param organizationId
     * @param authentication
     * @return Execution
     * @throws ResponseStatusException if execution is not found
     */
    @GetMapping("/api/latestExecution")
    fun getLatestExecutionForProject(@RequestParam name: String, @RequestParam organizationId: Long, authentication: Authentication): Mono<ExecutionDto> =
            justOrNotFound(
                executionService.getLatestExecutionByProjectNameAndProjectOrganizationId(name, organizationId),
                "Execution not found for project (name=$name, organization id=$organizationId)"
            )
                .filterWhen { projectPermissionEvaluator.checkPermissions(authentication, it, Permission.READ) }
                .map { it.toDto() }

    /**
     * Delete all executions by project name and project owner
     *
     * @param name name of project
     * @param organizationId organization of project
     * @param authentication
     * @return ResponseEntity
     * @throws ResponseStatusException
     */
    @PostMapping("/api/execution/deleteAll")
    @Suppress("UnsafeCallOnNullableType")
    fun deleteExecutionForProject(
        @RequestParam name: String,
        @RequestParam organizationId: Long,
        authentication: Authentication,
    ): Mono<ResponseEntity<*>> {
        val organization = organizationService.getOrganizationById(organizationId)
        return projectService.findWithPermissionByNameAndOrganization(
            authentication,
            name,
            organization,
            Permission.DELETE,
            messageIfNotFound = "Could not find the project with name: $name and owner: ${organization.name} or related objects",
        )
            .mapNotNull { it.id!! }
            .map { id ->
                testExecutionService.deleteTestExecutionWithProjectId(id)
                agentStatusService.deleteAgentStatusWithProjectId(id)
                agentService.deleteAgentWithProjectId(id)
                executionService.deleteExecutionByProjectNameAndProjectOrganization(name, organization)
                ResponseEntity.ok().build<String>()
            }
    }

    /**
     * FixMe: do we need to add preconditions that only executions from a single project can be deleted in a single query?
     *
     * @param executionIds list of ids
     * @param authentication
     * @return ResponseEntity
     * @throws ResponseStatusException
     */
    @PostMapping("/api/execution/delete")
    fun deleteExecutionsByExecutionIds(@RequestParam executionIds: List<Long>, authentication: Authentication): Mono<ResponseEntity<*>> = Flux.fromIterable(executionIds)
        .map { it to executionService.findExecution(it) }
        .filterAndInvoke({ (id, _) -> log.warn("Cannot delete execution id=$id because it's missing in the DB") }) { (_, execution) ->
            execution.isPresent
        }
        .map { (_, execution) -> execution.get() }
        .groupBy { it.project }
        .flatMap { groupedFlux ->
            val project = groupedFlux.key()
            groupedFlux.filterWhenAndInvoke({ log.warn("Cannot delete execution id=${it.id}, because operation is not allowed on project id=${project.id}") }) { execution ->
                projectPermissionEvaluator.checkPermissions(authentication, execution, Permission.DELETE)
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

    /**
     * Accepts a request to rerun an existing execution
     *
     * @param id id of an existing execution
     * @param authentication [Authentication] representing an authenticated request
     * @return bodiless response
     * @throws ResponseStatusException
     */
    @PostMapping("/api/rerunExecution")
    @Transactional
    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
    fun rerunExecution(@RequestParam id: Long, authentication: Authentication): Mono<String> {
        val execution = executionService.findExecution(id).orElseThrow {
            IllegalArgumentException("Can't rerun execution $id, because it does not exist")
        }
        if (!projectPermissionEvaluator.hasPermission(
            authentication, execution.project, Permission.WRITE
        )) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
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
}

