package com.saveourtool.save.orchestrator.controller

import com.saveourtool.save.agent.ExecutionLogs
import com.saveourtool.save.entities.Agent
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.BodilessResponseEntity
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.service.AgentService
import com.saveourtool.save.orchestrator.service.DockerService
import com.saveourtool.save.orchestrator.service.imageName

import com.github.dockerjava.api.exception.DockerClientException
import com.github.dockerjava.api.exception.DockerException
import io.fabric8.kubernetes.client.KubernetesClientException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.doOnError

import java.io.File

/**
 * Controller used to start agents with needed information
 */
@RestController
class AgentsController(
    private val agentService: AgentService,
    private val dockerService: DockerService,
    private val configProperties: ConfigProperties,
) {
    /**
     * Schedules tasks to build base images, create a number of containers and put their data into the database.
     *
     * @param execution
     * @return OK if everything went fine.
     * @throws ResponseStatusException
     */
    @Suppress("TOO_LONG_FUNCTION", "LongMethod", "UnsafeCallOnNullableType")
    @PostMapping("/initializeAgents")
    fun initialize(@RequestPart(required = true) execution: Execution): Mono<BodilessResponseEntity> {
        if (execution.status != ExecutionStatus.PENDING) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Execution status must be PENDING"
            )
        }
        val response = Mono.just(ResponseEntity<Void>(HttpStatus.ACCEPTED))
            .subscribeOn(agentService.scheduler)
        return response.doOnSuccess {
            log.info("Starting preparations for launching execution [project=${execution.project}, id=${execution.id}, " +
                    "status=${execution.status}, resourcesRootPath=${execution.resourcesRootPath}]")
            Mono.fromCallable {
                // todo: pass SDK via request body
                dockerService.buildBaseImage(execution)
            }
                .onErrorResume({ it is DockerException || it is DockerClientException }) { dex ->
                    reportExecutionError(execution, "Unable to build image and containers", dex)
                }
                .map { (baseImageId, agentRunCmd) ->
                    dockerService.createContainers(execution.id!!, baseImageId, agentRunCmd)
                }
                .onErrorResume({ it is DockerException || it is KubernetesClientException }) { ex ->
                    reportExecutionError(execution, "Unable to create docker containers", ex)
                }
                .flatMap { agentIds ->
                    agentService.saveAgentsWithInitialStatuses(
                        agentIds.map { id ->
                            Agent(id, execution)
                        }
                    )
                        .doOnError(WebClientResponseException::class) { exception ->
                            log.error("Unable to save agents, backend returned code ${exception.statusCode}", exception)
                            dockerService.cleanup(execution.id!!)
                        }
                        .doOnSuccess {
                            dockerService.startContainersAndUpdateExecution(execution, agentIds)
                        }
                }
                .subscribeOn(agentService.scheduler)
                .subscribe()
        }
    }

    private fun <T> reportExecutionError(
        execution: Execution,
        failReason: String,
        ex: Throwable?
    ): Mono<T> {
        log.error("$failReason for executionId=${execution.id}, will mark it as ERROR", ex)
        return execution.id?.let {
            agentService.updateExecution(it, ExecutionStatus.ERROR, failReason).then(Mono.empty())
        } ?: Mono.empty()
    }

    /**
     * @param agentIds list of IDs of agents to stop
     */
    @PostMapping("/stopAgents")
    fun stopAgents(@RequestBody agentIds: List<String>) {
        dockerService.stopAgents(agentIds)
    }

    /**
     * @param executionLogs ExecutionLogs
     */
    @PostMapping("/executionLogs")
    fun saveAgentsLog(@RequestBody executionLogs: ExecutionLogs) {
        val logDir = File(configProperties.executionLogs)
        if (!logDir.exists()) {
            log.info("Folder to store logs from agents was created: ${logDir.name}")
            logDir.mkdirs()
        }
        val logFile = File(logDir.path + File.separator + "${executionLogs.agentId}.log")
        if (!logFile.exists()) {
            logFile.createNewFile()
            log.info("Log file for ${executionLogs.agentId} agent was created")
        }
        logFile.appendText(executionLogs.cliLogs.joinToString(separator = System.lineSeparator(), postfix = System.lineSeparator()))
        log.info("Logs of agent id = ${executionLogs.agentId} were written")
    }

    /**
     * Delete containers and images associated with execution [executionId]
     *
     * @param executionId id of execution
     * @return empty response
     */
    @PostMapping("/cleanup")
    fun cleanup(@RequestParam executionId: Long) = Mono.fromCallable {
        dockerService.cleanup(executionId)
    }
        .doOnSuccess {
            dockerService.removeImage(imageName(executionId))
        }
        .flatMap {
            Mono.just(ResponseEntity<Void>(HttpStatus.OK))
        }

    companion object {
        private val log = LoggerFactory.getLogger(AgentsController::class.java)
    }
}
