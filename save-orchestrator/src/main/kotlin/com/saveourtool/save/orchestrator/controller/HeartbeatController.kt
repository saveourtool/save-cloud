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

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

internal val agentsLatestHeartBeatsMap: AgentStatesWithTimeStamps = ConcurrentHashMap()
internal val crashedAgents: MutableSet<String> = ConcurrentHashMap.newKeySet()

typealias AgentStatesWithTimeStamps = ConcurrentHashMap<String, Pair<String, Instant>>

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
        logger.info("Got heartbeat state: ${heartbeat.state.name} from ${heartbeat.agentId}")
        heartBeatInspector.updateAgentHeartbeatTimeStamps(heartbeat)

        // store new state into DB
        return agentService.updateAgentStatusesWithDto(
            listOf(
                AgentStatusDto(LocalDateTime.now(), heartbeat.state, heartbeat.agentId)
            )
        )
            .then(
                when (heartbeat.state) {
                    // if agent sends the first heartbeat, we try to assign work for it
                    STARTING -> handleVacantAgent(heartbeat.agentId, isStarting = true)
                    // if agent idles, we try to assign work, but also check if it should be terminated
                    IDLE -> handleVacantAgent(heartbeat.agentId, isStarting = false)
                    // if agent has finished its tasks, we check if all data has been saved and either assign new tasks or mark the previous batch as failed
                    FINISHED -> agentService.checkSavedData(heartbeat.agentId).flatMap { isSavingSuccessful ->
                        handleFinishedAgent(heartbeat.agentId, isSavingSuccessful)
                    }
                    BUSY -> Mono.just(ContinueResponse)
                    BACKEND_FAILURE, BACKEND_UNREACHABLE, CLI_FAILED, STOPPED_BY_ORCH -> Mono.just(WaitResponse)
                    CRASHED, TERMINATED -> {
                        handleIllegallyOnlineAgent(heartbeat.agentId, heartbeat.state)
                        Mono.just(WaitResponse)
                    }
                }
            )
            .map {
                Json.encodeToString(HeartbeatResponse.serializer(), it)
            }
    }

    private fun handleVacantAgent(agentId: String, isStarting: Boolean = false): Mono<HeartbeatResponse> =
            agentService.getNewTestsIds(agentId)
                .doOnSuccess {
                    if (it is NewJobResponse) {
                        agentService.updateAssignedAgent(agentId, it)
                    }
                }
                .zipWhen {
                    // Check if all agents have completed their jobs; after that we can terminate them.
                    // fixme: if orchestrator can shut down some agents while others are still doing work, this call won't be needed
                    if (it is WaitResponse && !isStarting) {
                        agentService.areAllAgentsIdleOrFinished(agentId)
                    } else {
                        Mono.just(false)
                    }
                }
                .flatMap { (response, shouldStop) ->
                    // to be more like the previous implementation, we wait for all agents to finish before returning
                    if (shouldStop) {
                        agentService.updateAgentStatusesWithDto(listOf(AgentStatusDto(LocalDateTime.now(), TERMINATED, agentId)))
                            .thenReturn(TerminateResponse)
                            .doOnSuccess {
                                logger.info("Agent id=$agentId will receive ${TerminateResponse::class.simpleName} and should shutdown gracefully")
                                ensureGracefulShutdown(agentId)
                            }
                    } else {
                        Mono.just(response)
                    }
                }

    private fun handleFinishedAgent(agentId: String, isSavingSuccessful: Boolean): Mono<HeartbeatResponse> = if (isSavingSuccessful) {
        handleVacantAgent(agentId, isStarting = false)
    } else {
        // Agent finished its work, however only part of results were received, other should be marked as failed
        agentService.markTestExecutionsAsFailed(listOf(agentId), FINISHED)
            .subscribeOn(agentService.scheduler)
            .subscribe()
        Mono.just(WaitResponse)
    }

    private fun handleIllegallyOnlineAgent(agentId: String, state: AgentState) {
        logger.warn("Agent id=$agentId sent $state status, but should be offline in that case!")
        crashedAgents.add(agentId)
    }

    private fun ensureGracefulShutdown(agentId: String) {
        val shutdownTimeout = configProperties.shutdown.gracefulTimeoutSeconds.seconds
        val numChecks: Int = configProperties.shutdown.gracefulNumChecks
        Flux.interval((shutdownTimeout / numChecks).toJavaDuration())
            .take(numChecks.toLong())
            .map {
                dockerService.isAgentStopped(agentId)
            }
            .takeUntil { it }
            // check whether we have got `true` or Flux has completed with only `false`
            .any { it }
            .doOnNext { successfullyStopped ->
                if (!successfullyStopped) {
                    logger.warn("Agent id=$agentId is not stopped in 60 seconds after ${TerminateResponse::class.simpleName} signal, will add it to crashed list")
                    crashedAgents.add(agentId)
                } else {
                    logger.debug("Agent id=$agentId has stopped after ${TerminateResponse::class.simpleName} signal")
                    agentsLatestHeartBeatsMap.remove(agentId)
                    crashedAgents.remove(agentId)
                }
                agentService.initiateShutdownSequence(agentId)
            }
            .subscribeOn(agentService.scheduler)
            .subscribe()
    }
}
