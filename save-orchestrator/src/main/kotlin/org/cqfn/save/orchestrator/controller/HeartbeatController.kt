package org.cqfn.save.orchestrator.controller

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
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
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean


private val agentsStartTimesMap: ConcurrentHashMap<String, Pair<String, LocalDateTime>> = ConcurrentHashMap<String, Pair<String, LocalDateTime>>()

private val agentsLatestHeartBeatsMap: ConcurrentHashMap<String, Pair<String, LocalDateTime>> = ConcurrentHashMap<String, Pair<String, LocalDateTime>>()
private val crashedAgentsList: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue<String>()
private val isHeartbeatInProgress = AtomicBoolean(false)

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
        if (isHeartbeatInProgress.compareAndSet(false, true)) {
            Foo(this).start()
        }

        logger.info("Got heartbeat state: ${heartbeat.state.name} from ${heartbeat.agentId}")
        updateAgentHeartbeatTimeStamps(heartbeat.agentId, heartbeat.state)

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
                    // if agent idles, we try to assign work, but also check if it should be terminated
                    AgentState.IDLE -> handleVacantAgent(heartbeat.agentId)
                    AgentState.FINISHED -> agentService.checkSavedData(heartbeat.agentId).flatMap { isSavingSuccessful ->
                        if (isSavingSuccessful) {
                            handleVacantAgent(heartbeat.agentId)
                        } else {
                            // todo: if failure is repeated multiple times, re-assign missing tests once more
                            Mono.just(WaitResponse)
                        }
                    }
                    AgentState.BUSY -> Mono.just(ContinueResponse)
                    AgentState.BACKEND_FAILURE, AgentState.BACKEND_UNREACHABLE, AgentState.CLI_FAILED, AgentState.STOPPED_BY_ORCH, AgentState.CRASHED -> Mono.just(WaitResponse)
                }
            )
            .map {
                Json.encodeToString(HeartbeatResponse.serializer(), it)
            }
    }

    private fun handleVacantAgent(agentId: String): Mono<HeartbeatResponse> = agentService.getNewTestsIds(agentId)
        .doOnSuccess {
            if (it is WaitResponse) {
                initiateShutdownSequence(agentId)
            }
        }


    fun updateAgentHeartbeatTimeStamps(agentId: String, state: AgentState) {
        val currentTime = LocalDateTime.now()
        if (state == AgentState.STARTING) {
            agentsStartTimesMap[agentId] = state.name to currentTime
        }
        agentsLatestHeartBeatsMap[agentId] = state.name to currentTime
    }

    fun determineCrashedAgents() {
        println("\n\n\nCURRENT AGENTS LIST:")
        agentsLatestHeartBeatsMap.forEach { (currentAgentId, stateToLatestHeartBeatPair) ->
            val duration = Duration.between(stateToLatestHeartBeatPair.second, LocalDateTime.now()).toMillis()
            if (duration >= configProperties.agentsHearBeatTimeoutMillis) {
                crashedAgentsList.add(currentAgentId)
            }
            println("agent ${currentAgentId}: ${stateToLatestHeartBeatPair.first} ${stateToLatestHeartBeatPair.second} DURATION $duration")
        }
    }

    fun processCrashedAgents() {
        if (crashedAgentsList.isEmpty()) {
            return
        }
        logger.debug("Start process hanging agents ${crashedAgentsList}")
        val areAgentsStopped = dockerService.stopAgents(crashedAgentsList)
        if (areAgentsStopped) {
            agentService.markAgentsAndTestExecutionsCrashed(crashedAgentsList)
            crashedAgentsList.clear()
        } else {
            logger.warn("Crashed agents $crashedAgentsList are not stopped after stop command")
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
                logger.debug("Waiting for ${configProperties.shutdownChecksIntervalMillis} ms to repeat `getAgentsAwaitingStop` call for agentId=$agentId")
                Mono.delay(Duration.ofMillis(configProperties.shutdownChecksIntervalMillis)).then(
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
                        agentsLatestHeartBeatsMap.clear()
                        logger.info("Agents have been stopped, will mark execution id=$executionId and agents $finishedAgentIds as FINISHED")
                        agentService
                            .markAgentsAndExecutionAsFinished(executionId, finishedAgentIds)
                    } else {
                        logger.warn("Agents $finishedAgentIds are not stopped after stop command")
                        Mono.empty()
                    }
                } else {
                    logger.debug("Agents other than $agentId are still running, so won't try to stop them")
                    Mono.empty()
                }
            }
            .subscribeOn(agentService.scheduler)
            .subscribe()
    }
}

class Foo(private val heartbeatController: HeartbeatController) : Thread() {
    //processCrashedAgents(mutableListOf(agentsStartTimesMap.toList().first().first))
    //processCrashedAgents(mutableListOf(heartbeat.agentId))
    override fun run() {
        while (true) {
            println("\n\n\nI'm Thread! My name is $name")
            heartbeatController.determineCrashedAgents()
            heartbeatController.processCrashedAgents()
            sleep(500)
        }
    }
}