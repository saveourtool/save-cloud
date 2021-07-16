package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.StringResponse
import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.backend.service.ExecutionService
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.ExecutionRequestForStandardSuites
import org.cqfn.save.entities.Project
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.execution.ExecutionType

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.LocalDateTime

/**
 * Controller to save project
 *
 * @property projectService service to manage projects
 *
 * @param configProperties configuration properties
 */
@RestController
class CloneRepositoryController(
    private val projectService: ProjectService,
    private val executionService: ExecutionService,
    configProperties: ConfigProperties,
) {
    private val log = LoggerFactory.getLogger(CloneRepositoryController::class.java)
    private val preprocessorWebClient = WebClient.create(configProperties.preprocessorUrl)

    /**
     * Endpoint to save project
     *
     * @param executionRequest information about project
     * @return mono string
     */
    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    @PostMapping(value = ["/submitExecutionRequest"])
    fun submitExecutionRequest(@RequestBody executionRequest: ExecutionRequest): Mono<StringResponse> {
        val projectExecution = executionRequest.project
        val project = projectService.getProjectByNameAndOwner(projectExecution.name, projectExecution.owner)
        return project?.let {
            executionRequest.executionId = saveExecution(it, ExecutionType.GIT)
            log.info("Sending request to preprocessor to start cloning project id=${it.id}")
            preprocessorWebClient
                .post()
                .uri("/upload")
                .body(Mono.just(executionRequest), ExecutionRequest::class.java)
                .retrieve()
                .toEntity(String::class.java)
                .toMono()
        } ?: Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Project doesn't exist"))
    }

    /**
     * Endpoint to save project as binary file
     *
     * @param executionRequestForStandardSuites information about project
     * @param propertyFile file with save properties
     * @param binaryFile binary project file
     * @return mono string
     */
    @PostMapping(value = ["/submitExecutionRequestBin"], consumes = ["multipart/form-data"])
    fun submitExecutionRequestByBin(
        @RequestPart("execution", required = true) executionRequestForStandardSuites: ExecutionRequestForStandardSuites,
        @RequestPart("property", required = true) propertyFile: Mono<FilePart>,
        @RequestPart("binFile", required = true) binaryFile: Mono<FilePart>,
    ): Mono<StringResponse> {
        val projectExecution = executionRequestForStandardSuites.project
        val project = projectService.getProjectByNameAndOwner(projectExecution.name, projectExecution.owner)
        project?.let {
            saveExecution(project, ExecutionType.STANDARD)
            log.info("Sending request to preprocessor to start save file for project id=${project.id}")
            val bodyBuilder = MultipartBodyBuilder()
            bodyBuilder.part("executionRequestForStandardSuites", executionRequestForStandardSuites)
            return Mono.zip(propertyFile, binaryFile).map {
                bodyBuilder.part("property", it.t1)
                bodyBuilder.part("binFile", it.t2)
            }.then(
                preprocessorWebClient
                    .post()
                    .uri("/uploadBin")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .toEntity(String::class.java)
                    .toMono())
        } ?: return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Project doesn't exist"))
    }

    private fun saveExecution(project: Project, type: ExecutionType): Long {
        val execution = Execution(project, LocalDateTime.now(), null, ExecutionStatus.PENDING, null,
            null, 0, null, type, null, 0, 0, 0)
        log.info("Creating a new execution id=${execution.id} for project id=${project.id}")
        return executionService.saveExecution(execution)
    }
}
