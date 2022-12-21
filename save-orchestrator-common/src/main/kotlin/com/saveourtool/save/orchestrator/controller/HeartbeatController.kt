/**
 * Heartbeat controller and corresponding logic which accepts heartbeat and depending on the state it returns the needed response
 */

package com.saveourtool.save.orchestrator.controller

import com.saveourtool.save.agent.*
import com.saveourtool.save.agent.AgentState.*
import com.saveourtool.save.entities.AgentDto
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.service.AgentService
import com.saveourtool.save.orchestrator.service.ContainerService
import com.saveourtool.save.orchestrator.service.HeartBeatInspector
import com.saveourtool.save.utils.*

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.doOnError
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono

import kotlin.time.Duration.Companion.seconds
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
     * 1. Response has STARTING state. Then orchestrator should register a new agent and send a configuration for new job.
     * 2. Response has IDLE state. Then orchestrator should send new jobs.
     * 3. Response has FINISHED state. Then orchestrator should send new jobs and validate that data has actually been saved successfully.
     * 4. Response has BUSY state. Then orchestrator sends an Empty response.
     * 5. Response has ERROR state. Then orchestrator sends a Wait response to assign another jobs
     *
     * @param heartbeat
     * @return Answer for agent
     */
    @PostMapping("/heartbeat")
    fun acceptHeartbeat(@RequestBody heartbeat: Heartbeat): Mono<String> {
        val executionId = heartbeat.executionProgress.executionId
        val containerId = heartbeat.agentInfo.containerId
        logger.info("Got heartbeat state: ${heartbeat.state.name} from $containerId under execution id=$executionId")
        return {
            containerService.markAgentForExecutionAsStarted(executionId)
            heartBeatInspector.updateAgentHeartbeatTimeStamps(heartbeat)
        }
            .toMono()
            .flatMap {
                // store new state into DB
                agentService.updateAgentStatus(
                    AgentStatusDto(heartbeat.state, containerId)
                )
            }
            .flatMap {
                when (heartbeat.state) {
                    // if agent sends the first heartbeat, we try to assign work for it
                    STARTING -> handleNewAgent(executionId, heartbeat.agentInfo)
                    // if agent idles, we try to assign work, but also check if it should be terminated
                    IDLE -> handleVacantAgent(containerId)
                    // if agent has finished its tasks, we check if all data has been saved and either assign new tasks or mark the previous batch as failed
                    FINISHED -> agentService.checkSavedData(containerId).flatMap { isSavingSuccessful ->
                        handleFinishedAgent(containerId, isSavingSuccessful)
                    }

                    BUSY -> Mono.just(ContinueResponse)
                    BACKEND_FAILURE, BACKEND_UNREACHABLE, CLI_FAILED -> Mono.just(WaitResponse)
                    CRASHED, TERMINATED -> Mono.fromCallable {
                        handleIllegallyOnlineAgent(containerId, heartbeat.state)
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

    private fun handleNewAgent(
        executionId: Long,
        agentInfo: AgentInfo,
    ): Mono<HeartbeatResponse> = agentService.addAgent(
        executionId = executionId,
        agent = AgentDto(
            containerId = agentInfo.containerId,
            containerName = agentInfo.containerName,
            version = agentInfo.version,
        )
    )
        .doOnError(WebClientResponseException::class) { exception ->
            log.error("Unable to save agents, backend returned code ${exception.statusCode}", exception)
            containerService.cleanupByExecutionId(executionId)
        }
        .then(agentService.getInitConfig(agentInfo.containerId))

    private fun handleVacantAgent(containerId: String): Mono<HeartbeatResponse> =
            agentService.getNextRunConfig(containerId)
                .asyncEffect {
                    agentService.updateAgentStatus(AgentStatusDto(BUSY, containerId))
                }
                .switchIfEmpty {
                    // Check if all agents have completed their jobs; if true - we can terminate agent [containerId].
                    // fixme: if orchestrator can shut down some agents while others are still doing work, this call won't be needed
                    // but maybe we'll want to keep running agents in case we need to re-run some tests on other agents e.g. in case of a crash.
                    agentService.areAllAgentsIdleOrFinished(containerId)
                        .filter { it }
                        .flatMap {
                            agentService.updateAgentStatus(AgentStatusDto(TERMINATED, containerId))
                                .thenReturn<HeartbeatResponse>(TerminateResponse)
                                .defaultIfEmpty(ContinueResponse)
                                .doOnSuccess {
                                    logger.info("Agent id=$containerId will receive ${TerminateResponse::class.simpleName} and should shutdown gracefully")
                                    ensureGracefulShutdown(containerId)
                                }
                        }
                        .defaultIfEmpty(WaitResponse)
                }

    private fun handleFinishedAgent(containerId: String, isSavingSuccessful: Boolean): Mono<HeartbeatResponse> = if (isSavingSuccessful) {
        handleVacantAgent(containerId)
    } else {
        // Agent finished its work, however only part of results were received, other should be marked as failed
        agentService.markReadyForTestingTestExecutionsOfAgentAsFailed(containerId)
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
        val numChecks = configProperties.shutdown.gracefulNumChecks
        waitReactivelyUntil(
            interval = shutdownTimeoutSeconds / numChecks,
            numberOfChecks = numChecks.toLong(),
        ) {
            containerService.isStopped(containerId)
        }
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

    companion object {
        private val log: Logger = getLogger<HeartbeatController>()
    }
}
