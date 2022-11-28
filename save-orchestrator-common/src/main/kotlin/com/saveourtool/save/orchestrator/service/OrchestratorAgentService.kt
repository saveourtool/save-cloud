package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.agent.AgentInitConfig
import com.saveourtool.save.agent.AgentRunConfig
import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.entities.*
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.BodilessResponseEntity
import com.saveourtool.save.test.TestBatch

import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

typealias IdList = List<Long>
typealias AgentStatusList = List<AgentStatusDto>
typealias TestExecutionList = List<TestExecutionDto>

/**
 * Repository to work with agents
 */
interface OrchestratorAgentService {
    /**
     * Gets config to init agent
     *
     * @param containerId
     * @return [Mono] of [AgentInitConfig]
     */
    fun getInitConfig(containerId: String): Mono<AgentInitConfig>

    /**
     * Gets new tests ids
     *
     * @param containerId
     * @return [Mono] of [TestBatch]
     */
    fun getNextRunConfig(containerId: String): Mono<AgentRunConfig>

    /**
     * Save new agents to the DB and insert their statuses. This logic is performed in two consecutive requests.
     *
     * @param agents list of [AgentDto]s to save in the DB
     * @return Mono with IDs of saved [Agent]s
     * @throws WebClientResponseException if any of the requests fails
     */
    fun addAgents(agents: List<AgentDto>): Mono<IdList>

    /**
     * @param agentStates list of [AgentStatusDto] to update/insert in the DB
     * @return a Mono without body
     */
    fun updateAgentStatusesWithDto(agentStates: List<AgentStatusDto>): Mono<BodilessResponseEntity>

    /**
     * Get List of [TestExecutionDto] for agent [containerId] have status READY_FOR_TESTING
     *
     * @param containerId agent for which data is checked
     * @return list of saved [TestExecutionDto]
     */
    fun getReadyForTestingTestExecutions(containerId: String): Mono<TestExecutionList>

    /**
     * Get list of [AgentStatus] for provided container ids
     *
     * @param containerIds ids of agents
     * @return Mono with response from backend
     */
    fun getAgentsStatuses(
        containerIds: List<String>,
    ): Mono<AgentStatusList>

    /**
     * Marks the execution to specified state
     *
     * @param executionId execution that should be updated
     * @param executionStatus new status for execution
     * @param failReason to show to user in case of error status
     * @return a Mono without body
     */
    fun updateExecutionByDto(
        executionId: Long,
        executionStatus: ExecutionStatus,
        failReason: String?,
    ): Mono<BodilessResponseEntity>

    /**
     * @param containerId containerId of an agent
     * @return Mono with [AgentStatusesForExecution]: agent statuses belonged to a single [com.saveourtool.save.entities.Execution]
     */
    fun getAgentsStatusesForSameExecution(containerId: String): Mono<AgentStatusesForExecution>

    /**
     * Mark agent's test executions as failed
     *
     * @param containerIds the list of agent IDs, for which, corresponding test executions should be marked as failed
     * @param onlyReadyForTesting mark only [TestExecution] with status [com.saveourtool.save.domain.TestResultStatus.READY_FOR_TESTING]
     * @return a Mono without body
     */
    fun markTestExecutionsOfAgentsAsFailed(containerIds: Collection<String>, onlyReadyForTesting: Boolean): Mono<BodilessResponseEntity>
}
