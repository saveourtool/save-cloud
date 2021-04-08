package org.cqfn.save.orchestrator.service

import org.cqfn.save.agent.AgentState
import org.cqfn.save.agent.HeartbeatResponse
import org.cqfn.save.agent.NewJobResponse
import org.cqfn.save.entities.Agent
import org.cqfn.save.entities.AgentStatus
import org.cqfn.save.orchestrator.config.ConfigProperties
import org.cqfn.save.test.TestDto

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
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
     * @return Mono<NewJobResponse>
     */
    fun setNewTestsIds(): Mono<out HeartbeatResponse> =
            webClientBackend
                .get()
                .uri("/getTestBatches")
                .retrieve()
                .bodyToMono(ParameterizedTypeReference.forType<List<TestDto>>(List::class.java))
                .map { NewJobResponse(it) }

    /**
     * Save new agents to the DB and insert their statuses. This logic is performed in two consecutive requests.
     *
     * @param agents list of [Agent]s to save in the DB
     * @throws WebClientResponseException if any of the requests fails
     */
    fun saveAgentsWithInitialStatuses(agents: List<Agent>) {
        webClientBackend
            .post()
            .uri("/addAgents")
            .body(BodyInserters.fromValue(agents))
            .retrieve()
            .bodyToMono(String::class.java)
        updateAgentStatuses(agents.map {
            AgentStatus(LocalDateTime.now(), AgentState.IDLE, it.agentId)
        })
    }

    /**
     * @param agentStates lsit of [AgentStatus]es to update in the DB
     */
    fun updateAgentStatuses(agentStates: List<AgentStatus>) {
        webClientBackend
            .post()
            .uri("/updateAgentStatuses")
            .body(BodyInserters.fromValue(agentStates))
            .retrieve()
            .bodyToMono(String::class.java)
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
}
