package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.StringResponse
import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.backend.repository.TimestampBasedFileSystemRepository
import org.cqfn.save.backend.service.ExecutionService
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.backend.utils.username
import org.cqfn.save.domain.FileInfo
import org.cqfn.save.domain.Sdk
import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.ExecutionRequestBase
import org.cqfn.save.entities.ExecutionRequestForStandardSuites
import org.cqfn.save.entities.Project
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.execution.ExecutionType
import org.cqfn.save.permission.Permission
import org.cqfn.save.v1

import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.lang.StringBuilder

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
    private val additionalToolsFileSystemRepository: TimestampBasedFileSystemRepository,
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
        projectService.findWithPermissionByNameAndOrganization(authentication, name, organization, Permission.WRITE)
    }
        .flatMap {
            sendToPreprocessor(
                executionRequest,
                ExecutionType.GIT,
                authentication.username(),
                files
            ) { newExecutionId ->
                part("executionRequest", executionRequest.copy(executionId = newExecutionId), MediaType.APPLICATION_JSON)
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
        projectService.findWithPermissionByNameAndOrganization(authentication, name, organization, Permission.WRITE)
    }
        .flatMap {
            sendToPreprocessor(
                executionRequestForStandardSuites,
                ExecutionType.STANDARD,
                authentication.username(),
                files
            ) {
                part("executionRequestForStandardSuites", executionRequestForStandardSuites, MediaType.APPLICATION_JSON)
            }
        }

    @Suppress("UnsafeCallOnNullableType")
    private fun sendToPreprocessor(
        executionRequest: ExecutionRequestBase,
        executionType: ExecutionType,
        username: String,
        files: Flux<FileInfo>,
        configure: MultipartBodyBuilder.(newExecutionId: Long) -> Unit
    ): Mono<StringResponse> {
        val project = with(executionRequest.project) {
            projectService.findByNameAndOrganizationName(name, organization.name)
        }
        return project?.let {
            val newExecution = saveExecution(project, username, executionType, configProperties.initialBatchSize, executionRequest.sdk)
            val newExecutionId = newExecution.id!!
            log.info("Sending request to preprocessor (executionType $executionType) to start save file for project id=${project.id}")
            val bodyBuilder = MultipartBodyBuilder().apply {
                configure(newExecutionId)
            }
            val uri = when (executionType) {
                ExecutionType.GIT -> "/upload"
                ExecutionType.STANDARD -> "/uploadBin"
            }
            files.collectToMultipartAndUpdateExecution(bodyBuilder, newExecution)
                .flatMap {
                    preprocessorWebClient.postMultipart(bodyBuilder, uri)
                }
        } ?: Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project doesn't exist"))
    }

    private fun saveExecution(
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
            id = executionService.saveExecution(this, username)
        }
        log.info("Creating a new execution id=${execution.id} for project id=${project.id}")
        return execution
    }

    private fun WebClient.postMultipart(bodyBuilder: MultipartBodyBuilder, uri: String) = post()
        .uri(uri)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
        .retrieve()
        .toEntity<String>()

    @Suppress("TYPE_ALIAS")
    private fun Flux<FileInfo>.collectToMultipartAndUpdateExecution(
        multipartBodyBuilder: MultipartBodyBuilder,
        execution: Execution
    ): Mono<List<MultipartBodyBuilder.PartBuilder>> {
        val additionalFiles = StringBuilder("")
        return map {
            val path = additionalToolsFileSystemRepository.getPath(it)
            additionalFiles.append("$path;")
            multipartBodyBuilder.part("fileInfo", it)
            multipartBodyBuilder.part("file", additionalToolsFileSystemRepository.getFile(it))
        }
            .collectList()
            .switchIfEmpty(Mono.just(emptyList()))
            .doOnNext {
                execution.additionalFiles = additionalFiles.toString()
                executionService.saveExecution(execution)
            }
    }
}
