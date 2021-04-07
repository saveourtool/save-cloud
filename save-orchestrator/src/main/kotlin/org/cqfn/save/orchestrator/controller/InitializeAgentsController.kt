package org.cqfn.save.orchestrator.controller

import org.cqfn.save.entities.Agent
import org.cqfn.save.entities.Execution
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.orchestrator.service.AgentService
import org.cqfn.save.orchestrator.service.DockerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

typealias Status = Mono<ResponseEntity<HttpStatus>>

/**
 * Controller used to starts agents with needed information
 */
@RestController
class InitializeAgentsController {
    @Autowired
    private lateinit var agentService: AgentService

    @Autowired
    private lateinit var dockerService: DockerService

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
        return Mono.just(ResponseEntity.ok(HttpStatus.OK))
            .subscribeOn(Schedulers.boundedElastic())
            .also {
                it.subscribe {
                    val agentIds = dockerService.buildAndCreateContainers(execution)
                    agentService.saveAgentsWithInitialStatuses(
                        agentIds.map { id ->
                            Agent(id, execution.id!!)
                        }
                    )
                }
            }
    }
}
