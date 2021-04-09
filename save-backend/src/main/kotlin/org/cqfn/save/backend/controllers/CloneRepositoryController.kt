package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.StringResponse
import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.entities.ExecutionRequest

import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

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
}
