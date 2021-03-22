package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.repository.GitRepository

import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * Controller to save project
 *
 * @property projectService service of project
 * @property webClient webclient
 */
@RestController
class CloneRepositoryController(
    private val projectService: ProjectService,
    private val webClient: WebClient,
) {
    private val log = LoggerFactory.getLogger(CloneRepositoryController::class.java)

    /**
     * Endpoint to save project
     *
     * @param executionRequest information about project
     * @return mono string
     */
    @PostMapping(value = ["/submitExecutionRequest"])
    fun submitExecutionRequest(@RequestBody executionRequest: ExecutionRequest): Mono<ResponseEntity<*>>? {
        val project = executionRequest.project
        try {
            projectService.saveProject(project)
            log.info("Project ${project.id} saved")
        } catch (exception: DataAccessException) {
            log.error("Save error with ${project.id} project", exception)
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error to clone repo"))
        }
        log.info("Starting to clone ${project.id} project")
        return webClient
            .post()
            .uri("/upload")
            .body(Mono.just(executionRequest.gitRepository), GitRepository::class.java)
            .retrieve()
            .bodyToMono(ResponseEntity::class.java)
    }
}
