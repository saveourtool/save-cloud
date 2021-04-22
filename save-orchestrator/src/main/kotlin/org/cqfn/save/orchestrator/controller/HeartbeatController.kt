package org.cqfn.save.orchestrator.controller

import org.cqfn.save.agent.AgentState
import org.cqfn.save.agent.ContinueResponse
import org.cqfn.save.agent.Heartbeat
import org.cqfn.save.agent.HeartbeatResponse
import org.cqfn.save.agent.WaitResponse
import org.cqfn.save.entities.AgentStatusDto
import org.cqfn.save.orchestrator.config.ConfigProperties
import org.cqfn.save.orchestrator.service.AgentService
import org.cqfn.save.orchestrator.service.DockerService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.LocalDateTime

/**
 * Controller for heartbeat
 *
 * @param agentService
 * @property configProperties
 */
@RestController
class HeartbeatController(private val agentService: AgentService,
                          private val dockerService: DockerService,
                          private val configProperties: ConfigProperties) {
    private val logger = LoggerFactory.getLogger(HeartbeatController::class.java)
    private val scheduler = Schedulers.boundedElastic().also { it.start() }

    /**
     * This controller accepts heartbeat and depending on the state it returns the needed response
     *
     * 1. Response has IDLE state. Then orchestrator should send new jobs.
     * 2. Response has FINISHED state. Then orchestrator should send new jobs and validate that data has actually been saved successfully.
     * 3. Response has BUSY state. Then orchestrator sends an Empty response.
     * 4. Response has ERROR state. Then orchestrator sends Terminating response.
     *
     * @param heartbeat
     * @return Answer for agent
     */
    @PostMapping("/heartbeat")
    fun acceptHeartbeat(@RequestBody heartbeat: Heartbeat): Mono<out HeartbeatResponse> {
        logger.info("Got heartbeat state: ${heartbeat.state.name} from ${heartbeat.agentId}")
        // store new state into DB
        agentService.updateAgentStatusesWithDto(
            listOf(
                AgentStatusDto(LocalDateTime.now(), LocalDateTime.now(), heartbeat.state, heartbeat.agentId)
            )
        )
        return when (heartbeat.state) {
            AgentState.IDLE -> agentService.setNewTestsIds().subscribeOn(scheduler)
                .doOnNext {
                    if (it is WaitResponse) {
                        // If agent was IDLE and there are no new tests - we check if the Execution is completed.
                        agentService.getAgentsAwaitingStop(heartbeat.agentId).subscribeOn(scheduler).subscribe { finishedAgentIds ->
                            if (finishedAgentIds.isNotEmpty()) {
                                dockerService.stopAgents(finishedAgentIds)
                            }
                        }
                    }
                }
                .apply { subscribe() }
            AgentState.FINISHED -> {
                agentService.checkSavedData()
                Mono.just(WaitResponse)
            }
            AgentState.BUSY -> Mono.just(ContinueResponse)
            AgentState.BACKEND_FAILURE -> Mono.just(WaitResponse)
            AgentState.BACKEND_UNREACHABLE -> Mono.just(WaitResponse)
            AgentState.CLI_FAILED -> Mono.just(WaitResponse)
        }
    }
}
