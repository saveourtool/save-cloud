package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.StringResponse
import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.entities.BinaryExecutionRequest
import org.cqfn.save.entities.ExecutionRequest

import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.io.File

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
    @PostMapping(value = ["/submitExecutionRequest"])
    fun submitExecutionRequest(@RequestBody executionRequest: ExecutionRequest): Mono<StringResponse> {
        val project = executionRequest.project
        val projectId: Long
        try {
            projectId = projectService.saveProject(project)
            executionRequest.project.id = projectId
            log.info("Project $projectId saved")
        } catch (exception: DataAccessException) {
            log.error("Error when saving project for execution request $executionRequest", exception)
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error to clone repo"))
        }
        log.info("Sending request to preprocessor to start cloning project id=$projectId")
        return preprocessorWebClient
            .post()
            .uri("/upload")
            .body(Mono.just(executionRequest), ExecutionRequest::class.java)
            .retrieve()
            .toEntity(String::class.java)
            .toMono()
    }

    /**
     * Endpoint to save project as binary file
     *
     * @param binaryExecutionRequest information about project
     * @param property file with save properties
     * @param binaryFile binary project file
     * @return mono string
     */
    @PostMapping(value = ["/submitExecutionRequestBin"], consumes = ["multipart/form-data"])
    fun submitExecutionRequestByBin(
        @RequestPart binaryExecutionRequest: BinaryExecutionRequest,
        @RequestPart("property", required = true) property: Mono<File>,
        @RequestPart("binFile", required = true) binaryFile: Mono<File>,
    ): Mono<StringResponse> {
        val project = binaryExecutionRequest.project
        val projectId: Long
        try {
            projectId = projectService.saveProject(project)
            binaryExecutionRequest.project.id = projectId
            log.info("Project $projectId saved")
        } catch (exception: DataAccessException) {
            log.error("Error when saving project for execution request $binaryExecutionRequest", exception)
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error to save binary file"))
        }
        log.info("Sending request to preprocessor to start save file for project id=$projectId")
        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("binaryExecutionRequest", binaryExecutionRequest)
        binaryFile.subscribe { binFile ->
            bodyBuilder.part("binFile", binFile)
            property.subscribe { propFile ->
                bodyBuilder.part("property", propFile)
            }
        }
        return preprocessorWebClient
            .post()
            .uri("/uploadBin")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .retrieve()
            .toEntity(String::class.java)
            .toMono()
    }
}
