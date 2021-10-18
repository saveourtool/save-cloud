package org.cqfn.save.orchestrator.controller

import org.cqfn.save.agent.ExecutionLogs
import org.cqfn.save.entities.Agent
import org.cqfn.save.entities.Execution
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.orchestrator.BodilessResponseEntity
import org.cqfn.save.orchestrator.config.ConfigProperties
import org.cqfn.save.orchestrator.service.AgentService
import org.cqfn.save.orchestrator.service.DockerService
import org.cqfn.save.orchestrator.service.imageName
import org.cqfn.save.testsuite.TestSuiteDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux.fromIterable
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.doOnError
import java.io.File

/**
 * Controller used to starts agents with needed information
 */
@RestController
class AgentsController {
    @Autowired
    private lateinit var agentService: AgentService

    @Autowired
    private lateinit var dockerService: DockerService

    @Autowired
    private lateinit var configProperties: ConfigProperties

    /**
     * Schedules tasks to build base images, create a number of containers and put their data into the database.
     *
     * @param execution
     * @param testSuiteDtos test suites, selected by user
     * @return OK if everything went fine.
     * @throws ResponseStatusException
     */
    @PostMapping("/initializeAgents")
    fun initialize(@RequestPart(required = true) execution: Execution,
                   @RequestPart(required = false) testSuiteDtos: List<TestSuiteDto>?): Mono<BodilessResponseEntity> {
        if (execution.status != ExecutionStatus.PENDING) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Execution status must be PENDING"
            )
        }
        val response = Mono.just(ResponseEntity<Void>(HttpStatus.ACCEPTED))
            .subscribeOn(agentService.scheduler)
        response.subscribe {
            log.info("Starting preparations for launching execution [project=${execution.project}, id=${execution.id}, " +
                    "status=${execution.status}, resourcesRootPath=${execution.resourcesRootPath}]")
            // todo: pass SDK via request body
            val agentIds = dockerService.buildAndCreateContainers(execution, testSuiteDtos)
            agentService.saveAgentsWithInitialStatuses(
                agentIds.map { id ->
                    Agent(id, execution)
                }
            )
                .doOnError(WebClientResponseException::class) { exception ->
                    log.error("Unable to save agents, backend returned code ${exception.statusCode}", exception)
                    agentIds.forEach {
                        dockerService.removeContainer(it)
                    }
                }
                .doOnSuccess {
                    dockerService.startContainersAndUpdateExecution(execution, agentIds)
                }
                .subscribeOn(agentService.scheduler)
                .subscribe()
        }
        return response
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
    fun cleanup(@RequestParam executionId: Long) =
            agentService.getAgentIdsForExecution(executionId)
                .flatMapMany(::fromIterable)
                .map { id ->
                    dockerService.removeContainer(id)
                }
                .doOnComplete {
                    dockerService.removeImage(imageName(executionId))
                }
                .collectList()
                .flatMap {
                    Mono.just(ResponseEntity<Void>(HttpStatus.OK))
                }

    companion object {
        private val log = LoggerFactory.getLogger(AgentsController::class.java)
    }
}
