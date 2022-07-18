package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.security.ProjectPermissionEvaluator
import com.saveourtool.save.backend.service.AgentService
import com.saveourtool.save.backend.service.AgentStatusService
import com.saveourtool.save.backend.service.ExecutionService
import com.saveourtool.save.backend.service.GitService
import com.saveourtool.save.backend.service.OrganizationService
import com.saveourtool.save.backend.service.ProjectService
import com.saveourtool.save.backend.service.TestExecutionService
import com.saveourtool.save.backend.service.TestSuitesService
import com.saveourtool.save.backend.storage.ExecutionInfoStorage
import com.saveourtool.save.backend.utils.justOrNotFound
import com.saveourtool.save.backend.utils.username
import com.saveourtool.save.core.utils.runIf
import com.saveourtool.save.domain.toSdk
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.ExecutionRequest
import com.saveourtool.save.entities.Project
import com.saveourtool.save.execution.ExecutionDto
import com.saveourtool.save.execution.ExecutionInitializationDto
import com.saveourtool.save.execution.ExecutionType
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.v1

import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
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
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

import java.util.concurrent.atomic.AtomicBoolean

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
                          private val executionInfoStorage: ExecutionInfoStorage,
                          config: ConfigProperties,
                          jackson2WebClientCustomizer: WebClientCustomizer,
) {
    private val log = LoggerFactory.getLogger(ExecutionController::class.java)
    private val preprocessorWebClient = WebClient.builder()
        .apply(jackson2WebClientCustomizer::customize)
        .baseUrl(config.preprocessorUrl)
        .build()

    /**
     * @param execution
     * @return id of created [Execution]
     */
    @PostMapping("/internal/createExecution")
    fun createExecution(@RequestBody execution: Execution): Long = executionService.saveExecutionAndReturnId(execution)

    /**
     * @param executionUpdateDto
     * @return empty Mono
     */
    @PostMapping("/internal/updateExecutionByDto")
    fun updateExecution(@RequestBody executionUpdateDto: ExecutionUpdateDto): Mono<Unit> = Mono.fromCallable {
        executionService.updateExecutionStatusById(executionUpdateDto.id, executionUpdateDto.status)
    }.flatMap {
        executionInfoStorage.upsertIfRequired(executionUpdateDto)
    }

    /**
     * Get execution by id
     *
     * @param id id of execution
     * @param authentication
     * @return execution if it has been found
     */
    @GetMapping(path = ["/api/$v1/execution", "/internal/execution"])
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
    @GetMapping(path = ["/api/$v1/executionDto"])
    fun getExecutionDto(@RequestParam executionId: Long, authentication: Authentication): Mono<ExecutionDto> =
            justOrNotFound(executionService.findExecution(executionId))
                .filterWhen { execution -> projectPermissionEvaluator.checkPermissions(authentication, execution, Permission.READ) }
                .map { it.toDto() }

    /**
     * @param name
     * @param authentication
     * @param organizationName
     * @return list of execution dtos
     * @throws NoSuchElementException
     */
    @GetMapping(path = ["/api/$v1/executionDtoList"])
    fun getExecutionByProject(@RequestParam name: String, @RequestParam organizationName: String, authentication: Authentication): Mono<List<ExecutionDto>> {
        val organization = organizationService.findByName(organizationName) ?: throw NoSuchElementException("Organization with name [$organizationName] was not found.")
        return projectService.findWithPermissionByNameAndOrganization(authentication, name, organization.name, Permission.READ).map {
            executionService.getExecutionDtoByNameAndOrganization(name, organization).reversed()
        }
    }

    /**
     * Get latest (by start time an) execution by project name and organization
     *
     * @param name project name
     * @param organizationName
     * @param authentication
     * @return Execution
     * @throws ResponseStatusException if execution is not found
     */
    @GetMapping(path = ["/api/$v1/latestExecution"])
    fun getLatestExecutionForProject(@RequestParam name: String, @RequestParam organizationName: String, authentication: Authentication): Mono<ExecutionDto> =
            Mono.justOrEmpty(
                executionService.getLatestExecutionByProjectNameAndProjectOrganizationName(name, organizationName)
            )
                .switchIfEmpty {
                    Mono.error(ResponseStatusException(HttpStatus.NO_CONTENT))
                }
                .filterWhen { projectPermissionEvaluator.checkPermissions(authentication, it, Permission.READ) }
                .map { it.toDto() }

    /**
     * Delete all executions by project name and organization
     *
     * @param name name of project
     * @param organizationName organization of project
     * @param authentication
     * @return ResponseEntity
     * @throws NoSuchElementException
     */
    @PostMapping(path = ["/api/$v1/execution/deleteAll"])
    @Suppress("UnsafeCallOnNullableType")
    fun deleteExecutionForProject(
        @RequestParam name: String,
        @RequestParam organizationName: String,
        authentication: Authentication,
    ): Mono<ResponseEntity<*>> {
        val organization = organizationService.findByName(organizationName) ?: throw NoSuchElementException("No such organization was found")
        return projectService.findWithPermissionByNameAndOrganization(
            authentication,
            name,
            organization.name,
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
     * Batch delete executions by a list of IDs.
     *
     * @param executionIds list of ids
     * @param authentication
     * @return ResponseEntity:
     * - status 200 if all executions have been deleted or some have been deleted and some are missing
     * - status 400 if all (not missing) executions don't belong to the same project
     * - status 403 if operation is not permitted **at least for some executions**
     * - status 404 if all executions are missing or the project is hidden from the current user
     * @throws ResponseStatusException
     */
    @PostMapping(path = ["/api/$v1/execution/delete"])
    @Suppress("TOO_LONG_FUNCTION", "NonBooleanPropertyPrefixedWithIs")
    fun deleteExecutionsByExecutionIds(@RequestParam executionIds: List<Long>, authentication: Authentication): Mono<ResponseEntity<*>> {
        val isProjectHidden = AtomicBoolean(false)
        return Flux.fromIterable(executionIds)
            .findPresentExecutions()
            .groupBy { it.project }
            .singleOrEmpty()
            .switchIfEmpty {
                Mono.error(ResponseStatusException(HttpStatus.BAD_REQUEST, "Some executions belong to different projects, can't delete all at once"))
            }
            .flatMapMany { groupedFlux: GroupedFlux<Project, Execution> ->
                val project = groupedFlux.key()
                groupedFlux.flatMap { execution ->
                    with(projectPermissionEvaluator) {
                        Mono.justOrEmpty(execution.project).filterByPermission(
                            authentication, Permission.DELETE, HttpStatus.FORBIDDEN
                        )
                    }
                        .map { execution }
                        .doOnError(ResponseStatusException::class.java) {
                            log.warn("Cannot delete execution id=${execution.id}, because operation is not allowed on project id=${project.id} (status ${it.status})")
                            if (it.status == HttpStatus.NOT_FOUND) {
                                isProjectHidden.set(true)
                            }
                        }
                }
            }
            .mapNotNull<Long> { it.id }
            .collectList()
            .map { filteredExecutionIds ->
                // at this point we should have only present executions from a project, that user has access to
                testExecutionService.deleteTestExecutionByExecutionIds(filteredExecutionIds)
                agentStatusService.deleteAgentStatusWithExecutionIds(filteredExecutionIds)
                agentService.deleteAgentByExecutionIds(filteredExecutionIds)
                executionService.deleteExecutionByIds(filteredExecutionIds)
                if (filteredExecutionIds.isNotEmpty()) {
                    ResponseEntity.ok().build<Void>()
                } else if (isProjectHidden.get()) {
                    // all executions belong to a project, that the user is not allowed to see
                    ResponseEntity.status(HttpStatus.NOT_FOUND).body("All executions are missing")
                } else {
                    ResponseEntity.status(HttpStatus.FORBIDDEN).build()
                }
            }
    }

    /**
     * @param id requested execution ID
     * @param authentication auth provider
     * @return string with a test root path that is linked with this execution id
     */
    @GetMapping(path = ["/api/$v1/getTestRootPathByExecutionId"])
    @Transactional
    fun getTestRootPathByExecutionId(@RequestParam id: Long, authentication: Authentication): Mono<String> =
            Mono.justOrEmpty(executionService.findExecution(id))
                .switchIfEmpty() {
                    Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
                }
                .filterWhen { projectPermissionEvaluator.checkPermissions(authentication, it, Permission.READ) }
                .map {
                    it.status.toString()
                    it.getTestRootPathByTestSuites()
                        .distinct()
                        .single()
                }

    /**
     * Accepts a request to rerun an existing execution
     *
     * @param id id of an existing execution
     * @param authentication [Authentication] representing an authenticated request
     * @return bodiless response
     * @throws ResponseStatusException
     */
    @PostMapping(path = ["/api/$v1/rerunExecution"])
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
            executionId = execution.id,
        )
        return preprocessorWebClient.post()
            .uri("/rerunExecution")
            .bodyValue(executionRequest)
            .retrieve()
            .bodyToMono()
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun Execution.getTestRootPathByTestSuites(): List<String> = this
        .parseAndGetTestSuiteIds()
        ?.map { testSuiteId ->
            testSuitesService.findTestSuiteById(testSuiteId).orElseThrow {
                log.error("Can't find test suite with id=$testSuiteId for executionId=$id")
                NoSuchElementException()
            }
        }
        ?.map {
            it.testRootPath
        }
        .orEmpty()

    /**
     * @param execution
     * @return the list of the testRootPaths for current execution; size of the list could be >1 only in standard mode
     */
    @PostMapping("/internal/findTestRootPathForExecutionByTestSuites")
    fun findTestRootPathByTestSuites(@RequestBody execution: Execution): List<String> = execution.getTestRootPathByTestSuites()

    /**
     * @return Flux of executions, that are present by ID; or `Flux.error` with status 404 if all executions are missing
     */
    private fun Flux<Long>.findPresentExecutions(): Flux<Execution> = collectMap({ id -> id }) { id -> executionService.findExecution(id) }
        .flatMapMany { idsToExecutions ->
            idsToExecutions.filterValues { it.isEmpty }.takeIf { it.isNotEmpty() }?.let { missingExecutions ->
                log.warn("Cannot delete executions with ids=${missingExecutions.keys} because they are missing in the DB")
                if (missingExecutions.size == idsToExecutions.size) {
                    return@flatMapMany Flux.error(ResponseStatusException(HttpStatus.NOT_FOUND, "All executions are missing"))
                }
            }
            Flux.fromIterable(idsToExecutions.mapValues { it.value.get() }.values)
        }
}
