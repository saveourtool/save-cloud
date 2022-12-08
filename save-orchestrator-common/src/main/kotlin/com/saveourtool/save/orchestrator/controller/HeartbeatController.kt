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
import com.saveourtool.save.orchestrator.service.HeartBeatInspector
import com.saveourtool.save.utils.*

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
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
                          private val heartBeatInspector: HeartBeatInspector,
) {
    private val logger = LoggerFactory.getLogger(HeartbeatController::class.java)

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
        logger.info("Got heartbeat state: ${heartbeat.state.name} from ${heartbeat.containerId} under execution id=$executionId")
        return {
            containerService.markAgentForExecutionAsStarted(executionId)
            heartBeatInspector.updateAgentHeartbeatTimeStamps(heartbeat)
        }
            .toMono()
            .flatMap {
                // store new state into DB
                agentService.updateAgentStatusesWithDto(
                    AgentStatusDto(heartbeat.state, heartbeat.containerId)
                )
            }
            .flatMap {
                when (heartbeat.state) {
                    // if agent sends the first heartbeat, we try to assign work for it
                    STARTING -> handleNewAgent(heartbeat.containerId)
                    // if agent idles, we try to assign work, but also check if it should be terminated
                    IDLE -> handleVacantAgent(heartbeat.containerId)
                    // if agent has finished its tasks, we check if all data has been saved and either assign new tasks or mark the previous batch as failed
                    FINISHED -> agentService.checkSavedData(heartbeat.containerId).flatMap { isSavingSuccessful ->
                        handleFinishedAgent(heartbeat.containerId, isSavingSuccessful)
                    }

                    BUSY -> Mono.just(ContinueResponse)
                    BACKEND_FAILURE, BACKEND_UNREACHABLE, CLI_FAILED -> Mono.just(WaitResponse)
                    CRASHED, TERMINATED, STOPPED_BY_ORCH -> Mono.fromCallable {
                        handleIllegallyOnlineAgent(heartbeat.containerId, heartbeat.state)
                        WaitResponse
                    }
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

    private fun handleVacantAgent(containerId: String): Mono<HeartbeatResponse> =
            agentService.getNextRunConfig(containerId)
                .asyncEffectIf({ this is NewJobResponse }) {
                    agentService.updateAgentStatusesWithDto(AgentStatusDto(BUSY, containerId))
                }
                .zipWhen {
                    // Check if all agents have completed their jobs; if true - we can terminate agent [containerId].
                    // fixme: if orchestrator can shut down some agents while others are still doing work, this call won't be needed
                    // but maybe we'll want to keep running agents in case we need to re-run some tests on other agents e.g. in case of a crash.
                    if (it is WaitResponse) {
                        agentService.areAllAgentsIdleOrFinished(containerId)
                    } else {
                        Mono.just(false)
                    }
                }
                .flatMap { (response, shouldStop) ->
                    if (shouldStop) {
                        agentService.updateAgentStatusesWithDto(AgentStatusDto(TERMINATED, containerId))
                            .thenReturn<HeartbeatResponse>(TerminateResponse)
                            .defaultIfEmpty(ContinueResponse)
                            .doOnSuccess {
                                logger.info("Agent id=$containerId will receive ${TerminateResponse::class.simpleName} and should shutdown gracefully")
                                ensureGracefulShutdown(containerId)
                            }
                    } else {
                        Mono.just(response)
                    }
                }

    private fun handleFinishedAgent(containerId: String, isSavingSuccessful: Boolean): Mono<HeartbeatResponse> = if (isSavingSuccessful) {
        handleVacantAgent(containerId)
    } else {
        // Agent finished its work, however only part of results were received, other should be marked as failed
        agentService.markTestExecutionsAsFailed(listOf(containerId), true)
            .subscribeOn(agentService.scheduler)
            .subscribe()
        Mono.just(WaitResponse)
    }

    private fun handleIllegallyOnlineAgent(containerId: String, state: AgentState) {
        logger.warn("Agent with containerId=$containerId sent $state status, but should be offline in that case!")
        heartBeatInspector.watchCrashedAgent(containerId)
    }

    private fun ensureGracefulShutdown(containerId: String) {
        val shutdownTimeoutSeconds = configProperties.shutdown.gracefulTimeoutSeconds.seconds
        val numChecks: Int = configProperties.shutdown.gracefulNumChecks
        Flux.interval((shutdownTimeoutSeconds / numChecks).toJavaDuration())
            .take(numChecks.toLong())
            .map {
                containerService.isStoppedByContainerId(containerId)
            }
            .takeUntil { it }
            // check whether we have got `true` or Flux has completed with only `false`
            .any { it }
            .doOnNext { successfullyStopped ->
                if (!successfullyStopped) {
                    logger.warn {
                        "Agent with containerId=$containerId is not stopped in $shutdownTimeoutSeconds seconds after ${TerminateResponse::class.simpleName} signal," +
                                " will add it to crashed list"
                    }
                    heartBeatInspector.watchCrashedAgent(containerId)
                } else {
                    logger.debug { "Agent with containerId=$containerId has stopped after ${TerminateResponse::class.simpleName} signal" }
                    heartBeatInspector.unwatchAgent(containerId)
                }
                // Update final execution status, perform cleanup etc.
                agentService.finalizeExecution(containerId)
            }
            .subscribeOn(agentService.scheduler)
            .subscribe()
    }
}
