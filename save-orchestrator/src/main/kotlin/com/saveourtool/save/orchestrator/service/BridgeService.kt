package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.agent.AgentState.*
import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.entities.Agent
import com.saveourtool.save.entities.AgentStatus
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.entities.AgentStatusesForExecution
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.BodilessResponseEntity
import com.saveourtool.save.test.TestBatch
import com.saveourtool.save.test.TestDto
import com.saveourtool.save.utils.*

import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

typealias IdList = List<Long>
typealias AgentStatusList = List<AgentStatusDto>
typealias TestExecutionList = List<TestExecutionDto>

/**
 * Service for work with agents and backend
 */
interface BridgeService {
    /**
     * Sets new tests ids
     *
     * @param agentId
     * @return Mono<NewJobResponse>
     */
    fun getNextTestBatch(agentId: String): Mono<TestBatch>

    /**
     * Save new agents to the DB and insert their statuses. This logic is performed in two consecutive requests.
     *
     * @param agents list of [Agent]s to save in the DB
     * @return Mono with IDs of saved [Agent]s
     * @throws WebClientResponseException if any of the requests fails
     */
    fun addAgents(agents: List<Agent>): Mono<IdList>

    /**
     * @param agentStates list of [AgentStatus]es to update in the DB
     * @return empty result
     */
    fun updateAgentStatuses(agentStates: List<AgentStatus>): Mono<Unit>

    /**
     * @param agentState [AgentStatus] to update in the DB
     * @return a Mono containing bodiless entity of response or an empty Mono if request has failed
     */
    fun updateAgentStatusesWithDto(agentState: AgentStatusDto): Mono<BodilessResponseEntity>

    /**
     * Check that no TestExecution for agent [agentId] have status READY_FOR_TESTING
     *
     * @param agentId agent for which data is checked
     * @return list of saved [TestExecutionDto]
     */
    fun getReadyForTestingTestExecutions(agentId: String): Mono<TestExecutionList>

    /**
     * Updates status of execution [executionId] based on statues of agents [agentIds]
     *
     * @param executionId id of an [Execution]
     * @param agentIds ids of agents
     * @return Mono with response from backend
     */
    fun getAgentsStatuses(
        executionId: Long,
        agentIds: List<String>,
    ): Mono<AgentStatusList>

    /**
     * Marks the execution to specified state
     *
     * @param executionId execution that should be updated
     * @param executionStatus new status for execution
     * @param failReason to show to user in case of error status
     * @return a bodiless response entity
     */
    fun updateExecutionByDto(
        executionId: Long,
        executionStatus: ExecutionStatus,
        failReason: String? = null
    ): Mono<BodilessResponseEntity>

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
    fun getAgentsStatusesForSameExecution(agentId: String): Mono<AgentStatusesForExecution>

    /**
     * @param agentId
     * @param testDtos
     * @return a bodiless response entity
     */
    fun assignAgent(agentId: String, testDtos: List<TestDto>): Mono<BodilessResponseEntity>

    /**
     * Mark agent's test executions as failed
     *
     * @param agentsList the list of agents, for which, according the [status] corresponding test executions should be marked as failed
     * @param status
     * @return a bodiless response entity
     */
    fun setStatusByAgentIds(agentsList: Collection<String>, status: AgentState): Mono<BodilessResponseEntity>
}
