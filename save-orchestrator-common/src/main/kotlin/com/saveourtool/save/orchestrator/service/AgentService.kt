package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.agent.*
import com.saveourtool.save.agent.AgentState.*
import com.saveourtool.save.entities.AgentDto
import com.saveourtool.save.entities.AgentStatus
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.utils.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientException
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.onErrorResume

/**
 * Service for work with agents and backend
 */
@Service
class AgentService(
    private val orchestratorAgentService: OrchestratorAgentService,
) {
    /**
     * A scheduler that executes long-running background tasks
     */
    internal val scheduler: Scheduler = Schedulers.boundedElastic().also { it.init() }

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
     * Mark agent's test executions as failed
     *
     * @param containerId the agent container IDs, for which, corresponding test executions should be marked as failed
     * @return a bodiless response entity
     */
    fun markReadyForTestingTestExecutionsOfAgentAsFailed(
        containerId: String,
    ): Mono<EmptyResponse> = orchestratorAgentService.markReadyForTestingTestExecutionsOfAgentAsFailed(containerId)

    companion object {
        private val log = LoggerFactory.getLogger(AgentService::class.java)
    }
}
