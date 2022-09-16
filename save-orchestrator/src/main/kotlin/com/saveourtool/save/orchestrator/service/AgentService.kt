package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.agent.*
import com.saveourtool.save.entities.Agent
import com.saveourtool.save.entities.AgentStatus
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.BodilessResponseEntity
import com.saveourtool.save.orchestrator.ExecutionIdToAgentIds
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler

/**
 * Service for work with agents and backend
 */
interface AgentService {
    /**
     * A scheduler that executes long-running background tasks
     */
    val scheduler: Scheduler

    /**
     * Sets new tests ids
     *
     * @param agentId
     * @return Mono<NewJobResponse>
     */
    fun getNewTestsIds(agentId: String): Mono<HeartbeatResponse>

    /**
     * Save new agents to the DB and insert their statuses. This logic is performed in two consecutive requests.
     *
     * @param agents list of [Agent]s to save in the DB
     * @return Mono with response body
     * @throws WebClientResponseException if any of the requests fails
     */
    fun saveAgentsWithInitialStatuses(agents: List<Agent>): Mono<Void>

    /**
     * @param agentStates list of [AgentStatus]es to update in the DB
     * @return Mono with response body
     */
    fun updateAgentStatuses(agentStates: List<AgentStatus>): Mono<Void>

    /**
     * @param agentState [AgentStatus] to update in the DB
     * @return a Mono containing bodiless entity of response or an empty Mono if request has failed
     */
    fun updateAgentStatusesWithDto(agentState: AgentStatusDto): Mono<BodilessResponseEntity>

    /**
     * Check that no TestExecution for agent [agentId] have status READY_FOR_TESTING
     *
     * @param agentId agent for which data is checked
     * @return true if all executions have status other than `READY_FOR_TESTING`
     */
    fun checkSavedData(agentId: String): Mono<Boolean>

    /**
     * This method should be called when all agents are done and execution status can be updated and cleanup can be performed
     *
     * @param agentId an ID of the agent from the execution, that will be checked.
     */
    fun finalizeExecution(agentId: String)

    /**
     * Updates status of execution [executionId] based on statues of agents [agentIds]
     *
     * @param executionId id of an [Execution]
     * @param agentIds ids of agents
     * @return Mono with response from backend
     */
    fun markExecutionBasedOnAgentStates(
        executionId: Long,
        agentIds: List<String>,
    ): Mono<BodilessResponseEntity>

    /**
     * Marks the execution to specified state
     *
     * @param executionId execution that should be updated
     * @param executionStatus new status for execution
     * @param failReason to show to user in case of error status
     * @return a bodiless response entity
     */
    fun updateExecution(executionId: Long, executionStatus: ExecutionStatus, failReason: String? = null): Mono<BodilessResponseEntity>

    /**
     * Get list of agent ids (containerIds) for agents that have completed their jobs.
     * If we call this method, then there are no unfinished TestExecutions. So we check other agents' status.
     *
     * We assume, that all agents will eventually have one of statuses are finished.
     * Situations when agent gets stuck with a different status and for whatever reason is unable to update
     * it, are not handled. Anyway, such agents should be eventually stopped by [HeartBeatInspector].
     *
     * @param agentId containerId of an agent
     * @return Mono with list of agent ids for agents that can be shut down for an executionId
     */
    fun getFinishedOrStoppedAgentsForSameExecution(agentId: String): Mono<ExecutionIdToAgentIds>

    /**
     * Checks whether all agent under one execution have completed their jobs.
     *
     * @param agentId containerId of an agent
     * @return true if all agents match finished status
     */
    fun areAllAgentsIdleOrFinished(agentId: String): Mono<Boolean>

    /**
     * Perform two operations in arbitrary order: assign `agentContainerId` agent to test executions
     * and mark this agent as BUSY
     *
     * @param agentContainerId id of an agent that receives tests
     * @param newJobResponse a heartbeat response with tests
     */
    fun updateAssignedAgent(agentContainerId: String, newJobResponse: NewJobResponse)

    /**
     * Mark agent's test executions as failed
     *
     * @param agentsList the list of agents, for which, according the [status] corresponding test executions should be marked as failed
     * @param status
     * @return a bodiless response entity
     */
    fun markTestExecutionsAsFailed(agentsList: Collection<String>, status: AgentState): Mono<BodilessResponseEntity>
}