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

import java.time.Duration
import java.time.LocalDateTime

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import reactor.core.Disposable
import reactor.core.publisher.Flux

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

    private val agentsLatestHeartBeatsMap: MutableMap<String, Pair<String, LocalDateTime>> = mutableMapOf()
    private val agentsStartTimesMap: MutableMap<String, Pair<String, LocalDateTime>> = mutableMapOf()

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
        // What if no heartbeats and all agents crashed, how to treat it?
        val crashedAgents = updateAgentHeartbeatTimeStamps(heartbeat.agentId, heartbeat.state)
        //processCrashedAgents(crashedAgents)
        processCrashedAgents(mutableListOf(heartbeat.agentId))

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


    fun updateAgentHeartbeatTimeStamps(agentId: String, state: AgentState): MutableList<String> {
        println("\n\n\nCURRENT AGENT $agentId ${state}")
        val currentTime = LocalDateTime.now()
        if (state == AgentState.STARTING) {
            agentsStartTimesMap[agentId] = state.name to currentTime
        }
        agentsLatestHeartBeatsMap[agentId] = state.name to currentTime
        println("CURRENT AGENTS LIST:")
        val crashedAgents: MutableList<String> = mutableListOf()
        agentsLatestHeartBeatsMap.forEach { (currentAgentId, stateToLatestHeartBeatPair) ->
            val duration = Duration.between(stateToLatestHeartBeatPair.second, currentTime).toMillis()
            if (duration >= configProperties.agentsHearBeatTimeoutMillis) {
                crashedAgents.add(currentAgentId)
            }
            println("agent ${currentAgentId}: ${stateToLatestHeartBeatPair.first} ${stateToLatestHeartBeatPair.second} DURATION $duration")
        }
        println("--------------------------------")
        return crashedAgents
    }

    fun processCrashedAgents(crashedAgents: MutableList<String>): Disposable {
        val areAgentsStopped = dockerService.stopAgents(crashedAgents)
        return if (areAgentsStopped) {
            //logger.info("Agents have been stopped, will mark execution id=$executionId and agents $finishedAgentIds as FINISHED")
            Flux.fromIterable(crashedAgents).flatMap {  agentId ->
                agentService.getExecutionByAgentId(agentId).map { execution ->
                    println("\n\n\nExecution id ${execution.id!!}")
                    // findTestsByAgentIdAndExecutionId
                    // TODO mark corresponding tests as failed
                    //Mono.just(execution)
                    execution
                }
            }.collectList()

        } else {
            logger.warn("Agents $crashedAgents are not stopped after stop command")
            Mono.empty()
        }
        .subscribeOn(agentService.scheduler)
        .subscribe()
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
