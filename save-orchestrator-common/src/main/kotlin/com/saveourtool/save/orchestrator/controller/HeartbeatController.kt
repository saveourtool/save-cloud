/**
 * Heartbeat controller and corresponding logic which accepts heartbeat and depending on the state it returns the needed response
 */

package com.saveourtool.save.orchestrator.controller

import com.saveourtool.save.agent.*
import com.saveourtool.save.agent.AgentState.*
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.service.AgentService
import com.saveourtool.save.orchestrator.service.ContainerService
import com.saveourtool.save.utils.*

import org.slf4j.Logger
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json

/**
 * Controller for heartbeat
 *
 * @param agentService
 * @property configProperties
 */
@RestController
class HeartbeatController(private val agentService: AgentService,
                          private val containerService: ContainerService,
                          private val configProperties: ConfigProperties,
) {
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
    fun acceptHeartbeat(@RequestBody heartbeat: Heartbeat): Mono<String> {
        val executionId = heartbeat.executionProgress.executionId
        val containerId = heartbeat.agentInfo.containerId
        log.info("Got heartbeat state: ${heartbeat.state.name} from $containerId under execution id=$executionId")
        return {
            containerService.updateAgentStatus(
                executionId = executionId,
                agentStatus = AgentStatusDto(
                    containerId = containerId,
                    state = heartbeat.state,
                    time = heartbeat.timestamp.toLocalDateTime(TimeZone.UTC),
                )
            )
        }
            .toMono()
            .flatMap {
                // store new state into DB
                agentService.updateAgentStatus(
                    AgentStatusDto(heartbeat.state, heartbeat.agentInfo.containerId)
                )
            }
            .flatMap {
                when (heartbeat.state) {
                    // if agent sends the first heartbeat, we try to assign work for it
                    STARTING -> handleNewAgent(containerId)
                    // if agent idles, we try to assign work, but also check if it should be terminated
                    IDLE -> handleVacantAgent(executionId, containerId)
                    // if agent has finished its tasks, we check if all data has been saved and either assign new tasks or mark the previous batch as failed
                    FINISHED -> agentService.checkSavedData(containerId).flatMap { isSavingSuccessful ->
                        handleFinishedAgent(executionId, containerId, isSavingSuccessful)
                    }
                    BUSY -> Mono.just(ContinueResponse)
                    BACKEND_FAILURE, BACKEND_UNREACHABLE, CLI_FAILED -> Mono.just(WaitResponse)
                    CRASHED, TERMINATED -> Mono.fromCallable {
                        log.warn("Agent with containerId=$containerId sent ${heartbeat.state} status, but should be offline in that case!")
                        containerService.markContainerAsCrashed(containerId)
                    }.thenReturn(TerminateResponse)
                }
            }
            // Heartbeat couldn't be processed, agent should replay it current state on the next heartbeat.
            .defaultIfEmpty(ContinueResponse)
            .map {
                Json.encodeToString(HeartbeatResponse.serializer(), it)
            }
    }

    private fun handleNewAgent(containerId: String): Mono<HeartbeatResponse> =
            agentService.getInitConfig(containerId)

    private fun handleVacantAgent(executionId: Long, containerId: String): Mono<HeartbeatResponse> =
            agentService.getNextRunConfig(containerId)
                .asyncEffect {
                    agentService.updateAgentStatus(AgentStatusDto(BUSY, containerId))
                }
                .switchIfEmpty {
                    // Check if all agents have completed their jobs; if true - we can terminate agent [containerId].
                    // fixme: if orchestrator can shut down some agents while others are still doing work, this call won't be needed
                    // but maybe we'll want to keep running agents in case we need to re-run some tests on other agents e.g. in case of a crash.
                    agentService.areAllAgentsIdleOrFinished(executionId)
                        .filter { it }
                        .flatMap {
                            agentService.updateAgentStatus(AgentStatusDto(TERMINATED, containerId))
                                .thenReturn<HeartbeatResponse>(TerminateResponse)
                                .defaultIfEmpty(ContinueResponse)
                                .doOnSuccess {
                                    log.info("Agent id=$containerId will receive ${TerminateResponse::class.simpleName} and should shutdown gracefully")
                                    containerService.ensureGracefullyStopped(executionId, containerId)
                                }
                        }
                        .defaultIfEmpty(WaitResponse)
                }

    private fun handleFinishedAgent(
        executionId: Long,
        containerId: String,
        isSavingSuccessful: Boolean
    ): Mono<HeartbeatResponse> = if (isSavingSuccessful) {
        handleVacantAgent(executionId, containerId)
    } else {
        // Agent finished its work, however only part of results were received, other should be marked as failed
        agentService.markReadyForTestingTestExecutionsOfAgentAsFailed(containerId)
            .subscribeOn(agentService.scheduler)
            .subscribe()
        Mono.just(WaitResponse)
    }

    companion object {
        private val log: Logger = getLogger<HeartbeatController>()
    }
}
