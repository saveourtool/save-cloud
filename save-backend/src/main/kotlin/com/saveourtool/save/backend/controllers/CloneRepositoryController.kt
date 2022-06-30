package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.StringResponse
import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.service.ExecutionService
import com.saveourtool.save.backend.service.ProjectService
import com.saveourtool.save.backend.utils.username
import com.saveourtool.save.domain.*
import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.domain.Sdk
import com.saveourtool.save.domain.toFileKey
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
        @RequestPart("file", required = false) files: Flux<FileInfo>,
        authentication: Authentication,
    ): Mono<StringResponse> = with(executionRequest.project) {
        // Project cannot be taken from executionRequest directly for permission evaluation:
        // it can be fudged by user, who submits it. We should get project from DB based on name/owner combination.
        projectService.findWithPermissionByNameAndOrganization(authentication, name, organization.name, Permission.WRITE)
    }
        .flatMap {
            sendToPreprocessor(
                executionRequest,
                ExecutionType.GIT,
                authentication.username(),
                files
            ) { executionRequest, savedExecution ->
                executionRequest.copy(executionId = savedExecution.requiredId())
            }
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
        @RequestPart("file", required = true) files: Flux<FileInfo>,
        authentication: Authentication,
    ): Mono<StringResponse> = with(executionRequestForStandardSuites.project) {
        projectService.findWithPermissionByNameAndOrganization(authentication, name, organization.name, Permission.WRITE)
    }
        .flatMap {
            sendToPreprocessor(
                executionRequestForStandardSuites,
                ExecutionType.STANDARD,
                authentication.username(),
                files
            ) { executionRequest, savedExecution ->
                executionRequest.copy(executionId = savedExecution.requiredId(), version = savedExecution.stubVersion())
            }
        }

    private fun <T : ExecutionRequestBase> sendToPreprocessor(
        executionRequest: T,
        executionType: ExecutionType,
        username: String,
        files: Flux<FileInfo>,
        updateExecutionInRequest: (T, Execution) -> T
    ): Mono<StringResponse> {
        val project = with(executionRequest.project) {
            projectService.findByNameAndOrganizationName(name, organization.name)
        } ?: return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project doesn't exist"))

        val newExecution = createNewExecution(project, username, executionType, configProperties.initialBatchSize, executionRequest.sdk)
        log.info("Sending request to preprocessor (executionType $executionType) to start save file for project id=${project.id}")
        val uri = when (executionType) {
            ExecutionType.GIT -> "/upload"
            ExecutionType.STANDARD -> "/uploadBin"
        }
        return files.updateExecution(newExecution).flatMap { savedExecution ->
            preprocessorWebClient.post()
                .uri(uri)
                .bodyValue(updateExecutionInRequest(executionRequest, savedExecution))
                .retrieve()
                .toEntity()
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

    // TODO: Save the proper version https://github.com/saveourtool/save-cloud/issues/321
    private fun Execution.stubVersion() = this.parseAndGetAdditionalFiles().first().name
}
