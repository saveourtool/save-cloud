package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.StringResponse
import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.service.ExecutionService
import com.saveourtool.save.backend.service.ProjectService
import com.saveourtool.save.backend.storage.FileStorage
import com.saveourtool.save.backend.utils.username
import com.saveourtool.save.domain.*
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.ExecutionRequest
import com.saveourtool.save.entities.ExecutionRequestBase
import com.saveourtool.save.entities.ExecutionRequestForStandardSuites
import com.saveourtool.save.entities.Project
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionType
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.v1

import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Controller to save project
 *
 * @property projectService service to manage projects
 * @property configProperties configuration properties
 */
@RestController
@RequestMapping("/api")
class CloneRepositoryController(
    private val projectService: ProjectService,
    private val executionService: ExecutionService,
    private val fileStorage: FileStorage,
    private val configProperties: ConfigProperties,
    jackson2WebClientCustomizer: WebClientCustomizer,
) {
    private val log = LoggerFactory.getLogger(CloneRepositoryController::class.java)
    private val preprocessorWebClient = WebClient.builder()
        .apply(jackson2WebClientCustomizer::customize)
        .baseUrl(configProperties.preprocessorUrl)
        .build()

    /**
     * Endpoint to save project
     *
     * @param executionRequest information about project
     * @param files resources for execution
     * @param authentication [Authentication] representing an authenticated request
     * @return mono string
     */
    @PostMapping(path = ["/$v1/submitExecutionRequest"], consumes = ["multipart/form-data"])
    fun submitExecutionRequest(
        @RequestPart(required = true) executionRequest: ExecutionRequest,
        @RequestPart("file", required = false) files: Flux<ShortFileInfo>,
        authentication: Authentication,
    ): Mono<StringResponse> =
            sendToPreprocessor(
                executionRequest,
                authentication,
                ExecutionType.GIT,
                files
            ) { request, executionId ->
                request.copy(executionId = executionId)
            }

    /**
     * Endpoint to save project as binary file
     *
     * @param executionRequestForStandardSuites information about project
     * @param files files required for execution
     * @param authentication [Authentication] representing an authenticated request
     * @return mono string
     */
    @PostMapping(path = ["/$v1/executionRequestStandardTests"], consumes = ["multipart/form-data"])
    fun executionRequestStandardTests(
        @RequestPart("execution", required = true) executionRequestForStandardSuites: ExecutionRequestForStandardSuites,
        @RequestPart("file", required = true) files: Flux<ShortFileInfo>,
        authentication: Authentication,
    ): Mono<StringResponse> =
            sendToPreprocessor(
                executionRequestForStandardSuites,
                authentication,
                ExecutionType.STANDARD,
                files
            ) { request, executionId ->
                request.copy(executionId = executionId)
            }

    private fun <T : ExecutionRequestBase> sendToPreprocessor(
        executionRequest: T,
        authentication: Authentication,
        executionType: ExecutionType,
        files: Flux<ShortFileInfo>,
        updateExecutionInRequest: (T, Long) -> T
    ): Mono<StringResponse> {
        return with(executionRequest.project) {
            projectService.findWithPermissionByNameAndOrganization(authentication, name, organization.name, Permission.WRITE)
        }.flatMap { project ->
            val newExecution = createNewExecution(
                project,
                authentication.username(),
                executionType,
                configProperties.initialBatchSize,
                executionRequest.sdk
            )
            log.info("Sending request to preprocessor (executionType $executionType) to start save file for project id=${project.id}")
            val uri = when (executionType) {
                ExecutionType.GIT -> "/upload"
                ExecutionType.STANDARD -> "/uploadBin"
            }
            val projectCoordinates = ProjectCoordinates(project.organization.name, project.name)
            return@flatMap fileStorage.convertToLatestFileInfo(projectCoordinates, files)
                .updateExecution(newExecution).flatMap { savedExecution ->
                preprocessorWebClient.post()
                    .uri(uri)
                    .bodyValue(updateExecutionInRequest(executionRequest, savedExecution.requiredId()))
                    .retrieve()
                    .toEntity()
            }
        }
    }

    private fun createNewExecution(
        project: Project,
        username: String,
        type: ExecutionType,
        batchSize: Int,
        sdk: Sdk,
    ): Execution {
        val execution = Execution.stub(project).apply {
            status = ExecutionStatus.PENDING
            this.batchSize = batchSize
            this.sdk = sdk.toString()
            this.type = type
            id = executionService.saveExecutionAndReturnId(this, username)
        }
        log.info("Creating a new execution id=${execution.id} for project id=${project.id}")
        return execution
    }

    private fun Flux<FileInfo>.updateExecution(
        execution: Execution,
    ): Mono<Execution> = map {
        it.toFileKey()
    }
        .collectList()
        .switchIfEmpty(Mono.just(emptyList()))
        .map { execution.formatAndSetAdditionalFiles(it) }
        .map { executionService.saveExecution(execution) }
}
