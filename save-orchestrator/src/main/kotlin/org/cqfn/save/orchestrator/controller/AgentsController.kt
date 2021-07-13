package org.cqfn.save.orchestrator.controller

import org.cqfn.save.agent.ExecutionLogs
import org.cqfn.save.entities.Agent
import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.Project
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.execution.ExecutionType
import org.cqfn.save.orchestrator.config.ConfigProperties
import org.cqfn.save.orchestrator.service.AgentService
import org.cqfn.save.orchestrator.service.DockerService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.File
import java.time.LocalDateTime

typealias Status = Mono<ResponseEntity<HttpStatus>>

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
     * @return OK if everything went fine.
     * @throws ResponseStatusException
     */
    @PostMapping("/initializeAgents")
    fun initialize(@RequestBody execution: Execution): Status {
        if (execution.status != ExecutionStatus.PENDING) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Execution status must be PENDING"
            )
        }
        val response = Mono.just(ResponseEntity.ok(HttpStatus.OK))
            .subscribeOn(Schedulers.boundedElastic())
        response.subscribe {
            log.info("Starting preparations for launching execution [project=${execution.project}, id=${execution.id}, " +
                    "status=${execution.status}, resourcesRootPath=${execution.resourcesRootPath}]")
            val agentIds = dockerService.buildAndCreateContainers(execution)
            agentService.saveAgentsWithInitialStatuses(
                agentIds.map { id ->
                    Agent(id, execution)
                }
            )
                .subscribe()
            dockerService.startContainersAndUpdateExecution(execution, agentIds)
        }
        return response
    }

    // todo: remove
    @PostMapping("/startTestAgent")
    fun buildTestContainers(): Mono<Mono<ServerResponse>> {
        return Mono.fromCallable { ServerResponse.ok().build() }.subscribeOn(Schedulers.boundedElastic()).doOnSuccess {
            val ids = dockerService.buildAndCreateContainers(
                Execution(
                    Project("test", "test", null, null).apply {
                        id = 48
                    },
                    LocalDateTime.now(),
                    null,
                    ExecutionStatus.PENDING,
                    "1,2,3",
                    "examples/kotlin-diktat",
                    0,
                    10,
                    ExecutionType.GIT,
                    "0.0.1",
                    0,
                    0,
                    0,
                ).apply {
                    id = 48
                }
            )
            ids.forEach { dockerService.containerManager.dockerClient.startContainerCmd(it).exec() }
        }
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

    companion object {
        private val log = LoggerFactory.getLogger(AgentsController::class.java)
    }
}
