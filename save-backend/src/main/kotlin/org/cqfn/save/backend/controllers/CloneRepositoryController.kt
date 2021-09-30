package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.StringResponse
import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.backend.repository.FileSystemRepository
import org.cqfn.save.backend.service.ExecutionService
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.domain.FileInfo
import org.cqfn.save.domain.Sdk
import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.ExecutionRequestBase
import org.cqfn.save.entities.ExecutionRequestForStandardSuites
import org.cqfn.save.entities.Project
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.execution.ExecutionType

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.file.Paths
import java.time.LocalDateTime

/**
 * Controller to save project
 *
 * @property projectService service to manage projects
 * @property configProperties configuration properties
 */
@RestController
class CloneRepositoryController(
    private val projectService: ProjectService,
    private val executionService: ExecutionService,
    private val fileSystemRepository: FileSystemRepository,
    private val configProperties: ConfigProperties,
) {
    private val log = LoggerFactory.getLogger(CloneRepositoryController::class.java)
    private val preprocessorWebClient = WebClient.create(configProperties.preprocessorUrl)

    /**
     * Endpoint to save project
     *
     * @param executionRequest information about project
     * @param files resources for execution
     * @return mono string
     */
    @PostMapping(value = ["/submitExecutionRequest"], consumes = ["multipart/form-data"])
    fun submitExecutionRequest(
        @RequestPart(required = true) executionRequest: ExecutionRequest,
        @RequestPart("file", required = false) files: Flux<FileInfo>,
    ): Mono<StringResponse> = sendToPreprocessor(
        executionRequest,
        ExecutionType.GIT,
        files
    ) { newExecutionId ->
        part("executionRequest", executionRequest.copy(executionId = newExecutionId))
    }

    /**
     * Endpoint to save project as binary file
     *
     * @param executionRequestForStandardSuites information about project
     * @param files files required for execution
     * @return mono string
     */
    @PostMapping(value = ["/submitExecutionRequestBin"], consumes = ["multipart/form-data"])
    fun submitExecutionRequestByBin(
        @RequestPart("execution", required = true) executionRequestForStandardSuites: ExecutionRequestForStandardSuites,
        @RequestPart("file", required = true) files: Flux<FileInfo>,
    ): Mono<StringResponse> = sendToPreprocessor(
        executionRequestForStandardSuites,
        ExecutionType.STANDARD,
        files
    ) {
        part("executionRequestForStandardSuites", executionRequestForStandardSuites)
    }

    private fun sendToPreprocessor(
        executionRequest: ExecutionRequestBase,
        executionType: ExecutionType,
        files: Flux<FileInfo>,
        configure: MultipartBodyBuilder.(newExecutionId: Long) -> Unit
    ): Mono<StringResponse> {
        val project = with(executionRequest.project) {
            projectService.getProjectByNameAndOwner(name, owner)
        }
        return project?.let {
            val newExecutionId = saveExecution(project, executionType, configProperties.initialBatchSize, executionRequest.sdk)
            log.info("Sending request to preprocessor (executionType $executionType) to start save file for project id=${project.id}")
            val bodyBuilder = MultipartBodyBuilder().apply {
                configure(newExecutionId)
            }
            val uri = when (executionType) {
                ExecutionType.GIT -> "/upload"
                ExecutionType.STANDARD -> "/uploadBin"
            }
            files.collectToMultipart(bodyBuilder)
                .flatMap {
                    preprocessorWebClient.postMultipart(bodyBuilder, uri)
                }
        } ?: Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project doesn't exist"))
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun saveExecution(
        project: Project,
        type: ExecutionType,
        batchSize: Int,
        sdk: Sdk
    ): Long {
        val execution = Execution(project, LocalDateTime.now(), null, ExecutionStatus.PENDING, null,
            null, 0, batchSize, type, null, 0, 0, 0, sdk.toString()).apply {
            id = executionService.saveExecution(this)
        }
        log.info("Creating a new execution id=${execution.id} for project id=${project.id}")
        return execution.id!!
    }

    private fun WebClient.postMultipart(bodyBuilder: MultipartBodyBuilder, uri: String) = post()
        .uri(uri)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
        .retrieve()
        .toEntity<String>()

    private fun Flux<FileInfo>.collectToMultipart(multipartBodyBuilder: MultipartBodyBuilder) = map {
        val path = Paths.get(it.uploadedMillis.toString()).resolve(it.name)
        println("\n\n\nFILE ${path}")
        multipartBodyBuilder.part("file", fileSystemRepository.getFile(it))
    }
        .collectList()
        .switchIfEmpty(Mono.just(emptyList()))
}
