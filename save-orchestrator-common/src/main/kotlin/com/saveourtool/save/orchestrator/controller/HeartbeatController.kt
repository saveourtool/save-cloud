/**
 * Heartbeat controller and corresponding logic which accepts heartbeat and depending on the state it returns the needed response
 */

package com.saveourtool.save.orchestrator.controller

import com.saveourtool.save.agent.*
import com.saveourtool.save.agent.AgentState.*
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.service.AgentService
import com.saveourtool.save.orchestrator.service.DockerService
import com.saveourtool.save.orchestrator.service.HeartBeatInspector
import com.saveourtool.save.utils.asyncEffectIf
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.newAgentStatus
import com.saveourtool.save.utils.warn

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

import java.time.LocalDateTime

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
                          private val dockerService: DockerService,
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
        logger.info("Got heartbeat state: ${heartbeat.state.name} from ${heartbeat.agentId} under execution id=$executionId")
        return {
            dockerService.markAgentForExecutionAsStarted(executionId)
            heartBeatInspector.updateAgentHeartbeatTimeStamps(heartbeat)
        }
            .toMono()
            .flatMap {
                // store new state into DB
                agentService.updateAgentStatusesWithDto(
                    AgentStatusDto(LocalDateTime.now(), heartbeat.state, heartbeat.agentId)
                )
            }
            .flatMap {
                when (heartbeat.state) {
                    // if agent sends the first heartbeat, we try to assign work for it
                    STARTING -> handleNewAgent(heartbeat.agentId)
                    // if agent idles, we try to assign work, but also check if it should be terminated
                    IDLE -> handleVacantAgent(heartbeat.agentId)
                    // if agent has finished its tasks, we check if all data has been saved and either assign new tasks or mark the previous batch as failed
                    FINISHED -> agentService.checkSavedData(heartbeat.agentId).flatMap { isSavingSuccessful ->
                        handleFinishedAgent(heartbeat.agentId, isSavingSuccessful)
                    }

                    BUSY -> Mono.just(ContinueResponse)
                    BACKEND_FAILURE, BACKEND_UNREACHABLE, CLI_FAILED -> Mono.just(WaitResponse)
                    CRASHED, TERMINATED, STOPPED_BY_ORCH -> Mono.fromCallable {
                        handleIllegallyOnlineAgent(heartbeat.agentId, heartbeat.state)
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

    private fun handleNewAgent(agentId: String): Mono<HeartbeatResponse> =
            agentService.getInitConfig(agentId)

    private fun handleVacantAgent(agentId: String): Mono<HeartbeatResponse> =
            agentService.getNextRunConfig(agentId)
                .asyncEffectIf({ this is NewJobResponse }) {
                    agentService.updateAgentStatusesWithDto(BUSY.newAgentStatus(agentId))
                }
                .zipWhen {
                    // Check if all agents have completed their jobs; if true - we can terminate agent [agentId].
                    // fixme: if orchestrator can shut down some agents while others are still doing work, this call won't be needed
                    // but maybe we'll want to keep running agents in case we need to re-run some tests on other agents e.g. in case of a crash.
                    if (it is WaitResponse) {
                        agentService.areAllAgentsIdleOrFinished(agentId)
                    } else {
                        Mono.just(false)
                    }
                }
                .flatMap { (response, shouldStop) ->
                    if (shouldStop) {
                        agentService.updateAgentStatusesWithDto(TERMINATED.newAgentStatus(agentId))
                            .thenReturn<HeartbeatResponse>(TerminateResponse)
                            .defaultIfEmpty(ContinueResponse)
                            .doOnSuccess {
                                logger.info("Agent id=$agentId will receive ${TerminateResponse::class.simpleName} and should shutdown gracefully")
                                ensureGracefulShutdown(agentId)
                            }
                    } else {
                        Mono.just(response)
                    }
                }

    private fun handleFinishedAgent(agentId: String, isSavingSuccessful: Boolean): Mono<HeartbeatResponse> = if (isSavingSuccessful) {
        handleVacantAgent(agentId)
    } else {
        // Agent finished its work, however only part of results were received, other should be marked as failed
        agentService.markTestExecutionsAsFailed(listOf(agentId), true)
            .subscribeOn(agentService.scheduler)
            .subscribe()
        Mono.just(WaitResponse)
    }

    private fun handleIllegallyOnlineAgent(agentId: String, state: AgentState) {
        logger.warn("Agent id=$agentId sent $state status, but should be offline in that case!")
        heartBeatInspector.watchCrashedAgent(agentId)
    }

    private fun ensureGracefulShutdown(agentId: String) {
        val shutdownTimeoutSeconds = configProperties.shutdown.gracefulTimeoutSeconds.seconds
        val numChecks: Int = configProperties.shutdown.gracefulNumChecks
        Flux.interval((shutdownTimeoutSeconds / numChecks).toJavaDuration())
            .take(numChecks.toLong())
            .map {
                dockerService.isAgentStopped(agentId)
            }
            .takeUntil { it }
            // check whether we have got `true` or Flux has completed with only `false`
            .any { it }
            .doOnNext { successfullyStopped ->
                if (!successfullyStopped) {
                    logger.warn {
                        "Agent id=$agentId is not stopped in $shutdownTimeoutSeconds seconds after ${TerminateResponse::class.simpleName} signal," +
                                " will add it to crashed list"
                    }
                    heartBeatInspector.watchCrashedAgent(agentId)
                } else {
                    logger.debug { "Agent id=$agentId has stopped after ${TerminateResponse::class.simpleName} signal" }
                    heartBeatInspector.unwatchAgent(agentId)
                }
                // Update final execution status, perform cleanup etc.
                agentService.finalizeExecution(agentId)
            }
            .subscribeOn(agentService.scheduler)
            .subscribe()
    }
}
