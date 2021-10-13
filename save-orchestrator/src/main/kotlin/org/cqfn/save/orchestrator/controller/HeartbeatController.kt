package org.cqfn.save.orchestrator.controller

import org.cqfn.save.agent.AgentState
import org.cqfn.save.agent.ContinueResponse
import org.cqfn.save.agent.Heartbeat
import org.cqfn.save.agent.NewJobResponse
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

import java.time.Duration
import java.time.LocalDateTime

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
    @OptIn(ExperimentalSerializationApi::class)
    fun acceptHeartbeat(@RequestBody heartbeat: Heartbeat): Mono<String> {
        logger.info("Got heartbeat state: ${heartbeat.state.name} from ${heartbeat.agentId}")
        // store new state into DB
        return agentService.updateAgentStatusesWithDto(
            listOf(
                AgentStatusDto(LocalDateTime.now(), heartbeat.state, heartbeat.agentId)
            )
        )
            .then(
                when (heartbeat.state) {
                    // if agent sends the first heartbeat, we try to assign work for it
                    AgentState.STARTING -> agentService.getNewTestsIds(heartbeat.agentId)
                        .doOnSuccess {
                            if (it is NewJobResponse) {
                                agentService.updateAssignedAgent(heartbeat.agentId, it)
                            }
                        }
                    // if agent idles, we try to assign work, but also check if it should be terminated
                    AgentState.IDLE -> agentService.getNewTestsIds(heartbeat.agentId)
                        .doOnSuccess {
                            if (it is WaitResponse) {
                                initiateShutdownSequence(heartbeat.agentId)
                            } else if (it is NewJobResponse) {
                                agentService.updateAssignedAgent(heartbeat.agentId, it)
                            }
                        }
                    AgentState.FINISHED -> {
                        agentService.checkSavedData()
                        Mono.just(WaitResponse)
                    }
                    AgentState.BUSY -> Mono.just(ContinueResponse)
                    AgentState.BACKEND_FAILURE -> Mono.just(WaitResponse)
                    AgentState.BACKEND_UNREACHABLE -> Mono.just(WaitResponse)
                    AgentState.CLI_FAILED -> Mono.just(WaitResponse)
                    AgentState.STOPPED_BY_ORCH -> Mono.just(WaitResponse)
                }
            )
            .map {
                Json.encodeToString(it)
            }
    }

    /**
     * If agent was IDLE and there are no new tests - we check if the Execution is completed.
     * We get all agents for the same execution, if they are all done.
     * Then we stop them via DockerService and update necessary statuses in DB via AgentService.
     *
     * @param agentId an ID of the agent from the execution, that will be checked.
     */
    private fun initiateShutdownSequence(agentId: String) {
        agentService.getAgentsAwaitingStop(agentId).flatMap { (_, finishedAgentIds) ->
            if (finishedAgentIds.isNotEmpty()) {
                // need to retry after some time, because for other agents BUSY state might have not been written completely
                logger.debug("Waiting for 5 seconds to repeat `getAgentsAwaitingStop` call for agentId=$agentId")
                Mono.delay(Duration.ofSeconds(5)).then(
                    agentService.getAgentsAwaitingStop(agentId)
                )
            } else {
                Mono.empty()
            }
        }
            .flatMap { (executionId, finishedAgentIds) ->
                if (finishedAgentIds.isNotEmpty()) {
                    logger.debug("Agents ids=$finishedAgentIds have completed execution, will make an attempt to terminate them")
                    val areAgentsStopped = dockerService.stopAgents(finishedAgentIds)
                    if (areAgentsStopped) {
                        logger.info("Agents have been stopped, will mark execution id=$executionId and agents $finishedAgentIds as FINISHED")
                        agentService
                            .markAgentsAndExecutionAsFinished(executionId, finishedAgentIds)
                    } else {
                        Mono.empty()
                    }
                } else {
                    Mono.empty()
                }
            }
            .subscribeOn(scheduler)
            .subscribe()
    }
}
