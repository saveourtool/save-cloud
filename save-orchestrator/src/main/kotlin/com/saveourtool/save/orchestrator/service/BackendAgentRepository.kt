package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.agent.AgentState.*
import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.entities.Agent
import com.saveourtool.save.entities.AgentStatus
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.entities.AgentStatusesForExecution
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.orchestrator.BodilessResponseEntity
import com.saveourtool.save.orchestrator.SANDBOX_PROFILE
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.test.TestBatch
import com.saveourtool.save.test.TestDto
import com.saveourtool.save.utils.*
import org.springframework.context.annotation.Profile

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

/**
 * Service for work with agents and backend
 */
@Profile("!$SANDBOX_PROFILE")
@Component
class BackendAgentRepository(
    configProperties: ConfigProperties,
) : AgentRepository {
    private val webClientBackend = WebClient.create(configProperties.backendUrl)

    /**
     * Sets new tests ids
     *
     * @param agentId
     * @return Mono<NewJobResponse>
     */
    override fun getNextTestBatch(agentId: String): Mono<TestBatch> = webClientBackend
        .get()
        .uri("/getTestBatches?agentId=$agentId")
        .retrieve()
        .bodyToMono()

    /**
     * Save new agents to the DB and insert their statuses. This logic is performed in two consecutive requests.
     *
     * @param agents list of [Agent]s to save in the DB
     * @return Mono with IDs of saved [Agent]s
     * @throws WebClientResponseException if any of the requests fails
     */
    override fun addAgents(agents: List<Agent>): Mono<IdList> = webClientBackend
        .post()
        .uri("/addAgents")
        .body(BodyInserters.fromValue(agents))
        .retrieve()
        .bodyToMono()

    /**
     * @param agentStates list of [AgentStatus]es to update in the DB
     * @return empty result
     */
    override fun updateAgentStatuses(agentStates: List<AgentStatus>): Mono<BodilessResponseEntity> = webClientBackend
        .post()
        .uri("/updateAgentStatuses")
        .body(BodyInserters.fromValue(agentStates))
        .retrieve()
        .toBodilessEntity()

    /**
     * @param agentState [AgentStatus] to update in the DB
     * @return a Mono containing bodiless entity of response or an empty Mono if request has failed
     */
    override fun updateAgentStatusesWithDto(agentState: AgentStatusDto): Mono<BodilessResponseEntity> =
            webClientBackend
                .post()
                .uri("/updateAgentStatusWithDto")
                .body(BodyInserters.fromValue(agentState))
                .retrieve()
                .toBodilessEntity()

    /**
     * Check that no TestExecution for agent [agentId] have status READY_FOR_TESTING
     *
     * @param agentId agent for which data is checked
     * @return list of saved [TestExecutionDto]
     */
    override fun getReadyForTestingTestExecutions(agentId: String): Mono<TestExecutionList> = webClientBackend.get()
        .uri("/testExecutions/agent/$agentId/${TestResultStatus.READY_FOR_TESTING}")
        .retrieve()
        .bodyToMono()

    /**
     * Updates status of execution [executionId] based on statues of agents [agentIds]
     *
     * @param executionId id of an [Execution]
     * @param agentIds ids of agents
     * @return Mono with response from backend
     */
    override fun getAgentsStatuses(
        executionId: Long,
        agentIds: List<String>,
    ): Mono<AgentStatusList> = webClientBackend
        .get()
        .uri("/agents/statuses?ids=${agentIds.joinToString(separator = DATABASE_DELIMITER)}")
        .retrieve()
        .bodyToMono()

    /**
     * Marks the execution to specified state
     *
     * @param executionId execution that should be updated
     * @param executionStatus new status for execution
     * @param failReason to show to user in case of error status
     * @return a bodiless response entity
     */
    override fun updateExecutionByDto(
        executionId: Long,
        executionStatus: ExecutionStatus,
        failReason: String?,
    ): Mono<BodilessResponseEntity> =
            webClientBackend.post()
                .uri("/updateExecutionByDto")
                .bodyValue(ExecutionUpdateDto(executionId, executionStatus, failReason))
                .retrieve()
                .toBodilessEntity()

    /**
     * Get list of agent ids (containerIds) for agents that have completed their jobs.
     * If we call this method, then there are no unfinished TestExecutions. So we check other agents' status.
     *
     * We assume, that all agents will eventually have one of statuses [areFinishedOrStopped].
     * Situations when agent gets stuck with a different status and for whatever reason is unable to update
     * it, are not handled. Anyway, such agents should be eventually stopped by [HeartBeatInspector].
     *
     * @param agentId containerId of an agent
     * @return Mono with [AgentStatusesForExecution]
     */
    override fun getAgentsStatusesForSameExecution(agentId: String): Mono<AgentStatusesForExecution> = webClientBackend
        .get()
        .uri("/getAgentsStatusesForSameExecution?agentId=$agentId")
        .retrieve()
        .bodyToMono()

    override fun assignAgent(agentId: String, testDtos: List<TestDto>): Mono<BodilessResponseEntity> = webClientBackend.post()
        .uri("/testExecution/assignAgent?agentContainerId=$agentId")
        .bodyValue(testDtos)
        .retrieve()
        .toBodilessEntity()

    /**
     * Mark agent's test executions as failed
     *
     * @param agentsIds the list of agents, for which, according the [status] corresponding test executions should be marked as failed
     * @param status
     * @return a bodiless response entity
     */
    override fun setStatusByAgentIds(agentsIds: Collection<String>, status: AgentState): Mono<BodilessResponseEntity> =
            webClientBackend.post()
                .uri("/testExecution/setStatusByAgentIds?status=${status.name}")
                .bodyValue(agentsIds)
                .retrieve()
                .toBodilessEntity()
}
