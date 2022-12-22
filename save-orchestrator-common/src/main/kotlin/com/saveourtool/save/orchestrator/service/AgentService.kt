package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.agent.*
import com.saveourtool.save.agent.AgentState.*
import com.saveourtool.save.entities.AgentDto
import com.saveourtool.save.entities.AgentStatus
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.runner.ContainerRunner
import com.saveourtool.save.orchestrator.utils.AgentStatusInMemoryRepository
import com.saveourtool.save.utils.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientException
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.onErrorResume
import java.time.Duration

/**
 * Service for work with agents and backend
 */
@Service
class AgentService(
    private val configProperties: ConfigProperties,
    private val containerRunner: ContainerRunner,
    private val orchestratorAgentService: OrchestratorAgentService,
    private val agentStatusInMemoryRepository: AgentStatusInMemoryRepository,
) {
    /**
     * A scheduler that executes long-running background tasks
     */
    internal val scheduler: Scheduler = Schedulers.boundedElastic().also { it.start() }

    /**
     * Gets configuration to init agent
     *
     * @param containerId
     * @return [Mono] of [InitResponse]
     */
    internal fun getInitConfig(containerId: String): Mono<HeartbeatResponse> =
            orchestratorAgentService.getInitConfig(containerId)
                .map { InitResponse(it) }

    /**
     * Sets new tests ids
     *
     * @param containerId
     * @return [Mono] of [NewJobResponse] if there is some job to do or [Mono.empty]
     */
    internal fun getNextRunConfig(containerId: String): Mono<HeartbeatResponse> =
            orchestratorAgentService.getNextRunConfig(containerId)
                .map { NewJobResponse(it) }

    /**
     * Save new agent to the DB
     *
     * @param executionId ID of an execution
     * @param agent [AgentDto] to save in the DB
     * @return Mono with response body
     */
    fun addAgent(
        executionId: Long,
        agent: AgentDto,
    ): Mono<EmptyResponse> = orchestratorAgentService.addAgent(executionId, agent)

    /**
     * @param agentStatus [AgentStatus] to update in the DB
     * @return a Mono containing bodiless entity of response or an empty Mono if request has failed
     */
    fun updateAgentStatus(agentStatus: AgentStatusDto): Mono<EmptyResponse> =
            orchestratorAgentService
                .updateAgentStatus(agentStatus)
                .onErrorResume(WebClientException::class) {
                    log.warn("Couldn't update agent statuses because of backend failure", it)
                    Mono.empty()
                }

    /**
     * Check that no TestExecution for agent [containerId] have status READY_FOR_TESTING
     *
     * @param containerId agent for which data is checked
     * @return true if all executions have status other than `READY_FOR_TESTING`
     */
    fun checkSavedData(containerId: String): Mono<Boolean> = orchestratorAgentService
        .getReadyForTestingTestExecutions(containerId)
        .map { it.isEmpty() }

    /**
     * This method should be called when all agents are done and execution status can be updated and cleanup can be performed
     *
     * @param executionId an ID of the execution, that will be checked.
     */
    @Suppress("TOO_LONG_FUNCTION", "AVOID_NULL_CHECKS")
    internal fun finalizeExecution(executionId: Long) {
        // Get a list of agents for this execution, if their statuses indicate that the execution can be terminated.
        // I.e., all agents must be stopped by this point in order to move further in shutdown logic.
        getFinishedOrStoppedAgentsByExecutionId(executionId)
            .filter { finishedContainerIds -> finishedContainerIds.isNotEmpty() }
            .flatMap {
                // need to retry after some time, because for other agents BUSY state might have not been written completely
                log.debug("Waiting for ${configProperties.shutdown.checksIntervalMillis} ms to repeat `getAgentsAwaitingStop` call for execution=$executionId")
                Mono.delay(Duration.ofMillis(configProperties.shutdown.checksIntervalMillis)).then(
                    getFinishedOrStoppedAgentsByExecutionId(executionId)
                )
            }
            .filter { finishedContainerIds -> finishedContainerIds.isNotEmpty() }
            .flatMap { finishedContainerIds ->
                log.info { "For execution id=$executionId all agents have completed their lifecycle" }
                markExecutionBasedOnAgentStates(executionId, finishedContainerIds)
                    .then(Mono.fromCallable {
                        agentStatusInMemoryRepository.deleteAllByExecutionId(executionId)
                        containerRunner.cleanupAllByExecution(executionId)
                    })
            }
            .doOnSuccess {
                if (it == null) {
                    log.debug("Agents for execution $executionId are still running, so won't try to stop them")
                }
            }
            .subscribeOn(scheduler)
            .subscribe()
    }

    /**
     * Updates status of execution [executionId] based on statues of agents [finishedContainerIds]
     *
     * @param executionId id of an [Execution]
     * @param finishedContainerIds ids of agents
     * @return Mono with response from backend
     */
    private fun markExecutionBasedOnAgentStates(
        executionId: Long,
        finishedContainerIds: List<String>,
    ): Mono<EmptyResponse> {
        // all { STOPPED_BY_ORCH || TERMINATED } -> FINISHED
        // all { CRASHED } -> ERROR; set all test executions to CRASHED
        return orchestratorAgentService
            .getAgentsStatuses(finishedContainerIds)
            .flatMap { agentStatuses ->
                // todo: take test execution statuses into account too
                if (agentStatuses.map { it.state }.all {
                    it == TERMINATED
                }) {
                    updateExecution(executionId, ExecutionStatus.FINISHED)
                } else if (agentStatuses.map { it.state }.all {
                    it == CRASHED
                }) {
                    updateExecution(executionId, ExecutionStatus.ERROR,
                        "All agents for this execution were crashed unexpectedly"
                    ).then(markAllTestExecutionsOfExecutionAsFailed(executionId))
                } else {
                    Mono.error(NotImplementedError("Updating execution (id=$executionId) status for agents with statuses $agentStatuses is not supported yet"))
                }
            }
    }

    /**
     * Marks the execution to specified state
     *
     * @param executionId execution that should be updated
     * @param executionStatus new status for execution
     * @param failReason to show to user in case of error status
     * @return a bodiless response entity
     */
    fun updateExecution(executionId: Long, executionStatus: ExecutionStatus, failReason: String? = null): Mono<EmptyResponse> =
            orchestratorAgentService.updateExecutionStatus(executionId, executionStatus, failReason)

    /**
     * Get list of agent ids (containerIds) for agents that have completed their jobs.
     * If we call this method, then there are no unfinished TestExecutions. So we check other agents' status.
     *
     * We assume, that all agents will eventually have one of statuses [areFinishedOrStopped].
     * Situations when agent gets stuck with a different status and for whatever reason is unable to update
     * it, are not handled. Anyway, such agents should be eventually stopped by [HeartBeatInspector].
     *
     * @param executionId containerId of an agent
     * @return Mono with list of agent ids for agents that can be shut down for an executionId
     */
    private fun getFinishedOrStoppedAgentsByExecutionId(executionId: Long): Mono<StringList> = orchestratorAgentService
        .getAgentStatusesByExecutionId(executionId)
        .map { agentStatuses ->
            log.debug { "For executionId=$executionId agent statuses are $agentStatuses" }
            // with new logic, should we check only for CRASHED, STOPPED, TERMINATED?
            if (agentStatuses.areFinishedOrStopped()) {
                log.debug("For execution id=$executionId there are finished or stopped agents")
                agentStatuses.map { it.containerId }
            } else {
                emptyList()
            }
        }

    /**
     * Checks whether all agent under one execution have completed their jobs.
     *
     * @param executionId ID of an execution
     * @return true if all agents match [areIdleOrFinished]
     */
    fun areAllAgentsIdleOrFinished(executionId: Long): Mono<Boolean> = orchestratorAgentService
        .getAgentStatusesByExecutionId(executionId)
        .map { agentStatuses ->
            log.debug("For executionId=$executionId agent statuses are $agentStatuses")
            agentStatuses.areIdleOrFinished()
        }

    /**
     * Mark agent's test executions as failed
     *
     * @param containerId the agent container IDs, for which, corresponding test executions should be marked as failed
     * @return a bodiless response entity
     */
    fun markReadyForTestingTestExecutionsOfAgentAsFailed(
        containerId: String,
    ): Mono<EmptyResponse> = orchestratorAgentService.markReadyForTestingTestExecutionsOfAgentAsFailed(containerId)

    /**
     * Mark agent's test executions as failed
     *
     * @param executionId the ID of execution, for which, corresponding test executions should be marked as failed
     * @return a bodiless response entity
     */
    fun markAllTestExecutionsOfExecutionAsFailed(
        executionId: Long,
    ): Mono<EmptyResponse> = orchestratorAgentService.markAllTestExecutionsOfExecutionAsFailed(executionId)

    private fun Collection<AgentStatusDto>.areIdleOrFinished() = all {
        it.state == IDLE || it.state == FINISHED || it.state == CRASHED || it.state == TERMINATED
    }

    private fun Collection<AgentStatusDto>.areFinishedOrStopped() = all {
        it.state == FINISHED || it.state == CRASHED || it.state == TERMINATED
    }

    companion object {
        private val log = LoggerFactory.getLogger(AgentService::class.java)
    }
}
