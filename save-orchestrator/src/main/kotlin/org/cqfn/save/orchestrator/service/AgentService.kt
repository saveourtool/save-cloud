package org.cqfn.save.orchestrator.service

import org.cqfn.save.agent.AgentState
import org.cqfn.save.agent.HeartbeatResponse
import org.cqfn.save.agent.NewJobResponse
import org.cqfn.save.agent.WaitResponse
import org.cqfn.save.entities.Agent
import org.cqfn.save.entities.AgentStatus
import org.cqfn.save.entities.AgentStatusDto
import org.cqfn.save.orchestrator.config.ConfigProperties
import org.cqfn.save.test.TestDto

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

import java.time.LocalDateTime

/**
 * Service for work with agents and backend
 */
@Service
class AgentService(configProperties: ConfigProperties) {
    @Autowired
    @Qualifier("webClientBackend")
    private lateinit var webClientBackend: WebClient

    /**
     * Sets new tests ids
     *
     * @param agentId
     * @return Mono<NewJobResponse>
     */
    fun setNewTestsIds(agentId: String): Mono<out HeartbeatResponse> =
            webClientBackend
                .get()
                .uri("/getTestBatches?agentId=$agentId")
                .retrieve()
                .bodyToMono<List<TestDto>>()
                .map {
                    if (it.isNotEmpty()) {
                        NewJobResponse(it)
                    } else {
                        WaitResponse
                    }
                }

    /**
     * Save new agents to the DB and insert their statuses. This logic is performed in two consecutive requests.
     *
     * @param agents list of [Agent]s to save in the DB
     * @throws WebClientResponseException if any of the requests fails
     */
    fun saveAgentsWithInitialStatuses(agents: List<Agent>): Mono<Void> {
        return webClientBackend
            .post()
            .uri("/addAgents")
            .body(BodyInserters.fromValue(agents))
            .retrieve()
            .bodyToMono<Void>()
            .doOnSuccess {
                updateAgentStatuses(agents.map {
                    AgentStatus(LocalDateTime.now(), LocalDateTime.now(), AgentState.IDLE, it)
                })
            }
    }

    /**
     * @param agentStates list of [AgentStatus]es to update in the DB
     */
    fun updateAgentStatuses(agentStates: List<AgentStatus>): Mono<String> {
        return webClientBackend
            .post()
            .uri("/updateAgentStatuses")
            .body(BodyInserters.fromValue(agentStates))
            .retrieve()
            .bodyToMono()
    }

    /**
     * @param agentStates list of [AgentStatus]es to update in the DB
     */
    fun updateAgentStatusesWithDto(agentStates: List<AgentStatusDto>) {
        webClientBackend
            .post()
            .uri("/updateAgentStatusesWithDto")
            .body(BodyInserters.fromValue(agentStates))
            .retrieve()
            .bodyToMono<String>()
    }

    /**
     * @return nothing for now Fixme
     */
    @Suppress("FunctionOnlyReturningConstant")
    fun checkSavedData() = true

    /**
     * If an error occurs, should try to resend tests
     */
    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")  // Fixme
    fun resendTestsOnError() {
        TODO()
    }

    /**
     * Get list of agent ids (containerIds) for agents that have completed their jobs.
     *
     * @param agentId containerId of an agent
     * @return Mono with list of agent ids for agents that can be shut down.
     */
    fun getAgentsAwaitingStop(agentId: String): Mono<List<String>> {
        // If we call this method, then there are no unfinished TestExecutions.
        // check other agents status
        return webClientBackend
            .get()
            .uri("/getAgentsStatusesForSameExecution")
            .retrieve()
            .bodyToMono<List<AgentStatusDto>>()
            .map { agentStatuses ->
                if (agentStatuses.all { it.state == AgentState.IDLE }) {
                    agentStatuses.map { it.containerId }
                } else {
                    emptyList()
                }
            }
    }
}
