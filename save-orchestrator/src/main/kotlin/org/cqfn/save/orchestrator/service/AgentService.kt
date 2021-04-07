package org.cqfn.save.orchestrator.service

import org.cqfn.save.agent.HeartbeatResponse
import org.cqfn.save.agent.NewJobResponse
import org.cqfn.save.orchestrator.config.ConfigProperties
import org.cqfn.save.test.TestDto

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.cqfn.save.agent.AgentState
import org.cqfn.save.entities.Agent
import org.cqfn.save.entities.AgentStatus
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.LocalDateTime

/**
 * Service for work with agents and backend
 *
 * @param baseUrl
 */
@Service
class AgentService(configProperties: ConfigProperties) {
    /**
     * Used to send requests to backend
     */
    private val webClient = WebClient.create(configProperties.backendUrl)

    /**
     * Sets new tests ids
     *
     * @return Mono<NewJobResponse>
     */
    fun setNewTestsIds(): Mono<out HeartbeatResponse> =
            webClient
                .get()
                .uri("/getTestBatches")
                .retrieve()
                .bodyToMono(String::class.java)
                .map {
                    val listTest: List<TestDto> = Json.decodeFromString(it)
                    NewJobResponse(listTest)
                }

    /**
     * Save new agents to the DB and insert their statuses. This logic is performed in two consecutive requests.
     *
     * @throws WebClientResponseException if any of the requests fails
     */
    fun saveAgentsWithInitialStatuses(agents: List<Agent>) {
        webClient
            .post()
            .uri("/addAgents")
            .bodyValue(Json.encodeToString(agents))
            .retrieve()
            .bodyToMono(String::class.java)
        updateAgentStates(agents.map {
            AgentStatus(LocalDateTime.now(), AgentState.IDLE, it.agentId)
        })
    }

    fun updateAgentStates(agentStates: List<AgentStatus>) {
        webClient
            .post()
            .uri("/updateAgentStates")
            .bodyValue(Json.encodeToString(agentStates))
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
