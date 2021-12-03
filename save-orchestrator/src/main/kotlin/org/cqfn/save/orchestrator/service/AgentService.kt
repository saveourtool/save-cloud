package org.cqfn.save.orchestrator.service

import org.cqfn.save.agent.AgentState
import org.cqfn.save.agent.HeartbeatResponse
import org.cqfn.save.agent.NewJobResponse
import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.agent.WaitResponse
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.entities.Agent
import org.cqfn.save.entities.AgentStatus
import org.cqfn.save.entities.AgentStatusDto
import org.cqfn.save.entities.AgentStatusesForExecution
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.execution.ExecutionUpdateDto
import org.cqfn.save.orchestrator.BodilessResponseEntity
import org.cqfn.save.test.TestBatch
import org.cqfn.save.test.TestDto

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.Loggers

import java.time.LocalDateTime
import java.util.logging.Level

/**
 * Service for work with agents and backend
 */
@Service
class AgentService {
    /**
     * A scheduler that executes long-running background tasks
     */
    internal val scheduler = Schedulers.boundedElastic().also { it.start() }

    @Autowired
    @Qualifier("webClientBackend")
    private lateinit var webClientBackend: WebClient

    /**
     * Sets new tests ids
     *
     * @param agentId
     * @return Mono<NewJobResponse>
     */
    fun getNewTestsIds(agentId: String): Mono<HeartbeatResponse> =
            webClientBackend
                .get()
                .uri("/getTestBatches?agentId=$agentId")
                .retrieve()
                .bodyToMono<TestBatch>()
                .map { batch -> batch.toHeartbeatResponse(agentId) }
                .doOnSuccess {
                    if (it is NewJobResponse) {
                        updateAssignedAgent(agentId, it)
                    }
                }

    /**
     * Save new agents to the DB and insert their statuses. This logic is performed in two consecutive requests.
     *
     * @param agents list of [Agent]s to save in the DB
     * @return Mono with response body
     * @throws WebClientResponseException if any of the requests fails
     */
    fun saveAgentsWithInitialStatuses(agents: List<Agent>): Mono<Void> = webClientBackend
        .post()
        .uri("/addAgents")
        .body(BodyInserters.fromValue(agents))
        .retrieve()
        .bodyToMono<List<Long>>()
        .log(log, Level.INFO, true)
        .flatMap { agentIds ->
            updateAgentStatuses(agents.zip(agentIds).map { (agent, id) ->
                AgentStatus(LocalDateTime.now(), LocalDateTime.now(), AgentState.STARTING, agent.also { it.id = id })
            })
        }

    /**
     * @param agentStates list of [AgentStatus]es to update in the DB
     * @return Mono with response body
     */
    fun updateAgentStatuses(agentStates: List<AgentStatus>): Mono<Void> = webClientBackend
        .post()
        .uri("/updateAgentStatuses")
        .body(BodyInserters.fromValue(agentStates))
        .retrieve()
        .bodyToMono<Void>()
        .log(log, Level.INFO, true)

    /**
     * @param agentStates list of [AgentStatus]es to update in the DB
     * @return as bodiless entity of response
     */
    fun updateAgentStatusesWithDto(agentStates: List<AgentStatusDto>): Mono<BodilessResponseEntity> =
            webClientBackend
                .post()
                .uri("/updateAgentStatusesWithDto")
                .body(BodyInserters.fromValue(agentStates))
                .retrieve()
                .toBodilessEntity()

    /**
     * Check that no TestExecution for agent [agentId] have status READY_FOR_TESTING
     *
     * @param agentId agent for which data is checked
     * @return true if all executions have status other than `READY_FOR_TESTING`
     */
    fun checkSavedData(agentId: String): Mono<Boolean> = webClientBackend.get()
        .uri("/testExecutions/agent/$agentId/${TestResultStatus.READY_FOR_TESTING}")
        .retrieve()
        .bodyToMono<List<TestExecutionDto>>()
        .map { it.isEmpty() }

    /**
     * If an error occurs, should try to resend tests
     */
    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")  // Fixme
    fun resendTestsOnError() {
        TODO()
    }

    /**
     * Marks agent states and then execution state as FINISHED
     *
     * @param executionId execution that should be updated
     * @param finishedAgentIds agents that should be updated
     * @return a bodiless response entity
     */
    fun markAgentsAndExecutionAsFinished(executionId: Long, finishedAgentIds: List<String>): Mono<BodilessResponseEntity> =
            updateAgentStatusesWithDto(
                finishedAgentIds.map { agentId ->
                    AgentStatusDto(LocalDateTime.now(), AgentState.STOPPED_BY_ORCH, agentId)
                }
            )
                .then(
                    updateExecution(executionId, ExecutionStatus.FINISHED)  // todo: status based on results
                )

    /**
     * Marks the execution to specified state
     *
     * @param executionId execution that should be updated
     * @param executionStatus new status for execution
     * @return a bodiless response entity
     */
    fun updateExecution(executionId: Long, executionStatus: ExecutionStatus) =
            webClientBackend.post()
                .uri("/updateExecutionByDto")
                .bodyValue(ExecutionUpdateDto(executionId, executionStatus))
                .retrieve()
                .toBodilessEntity()

    /**
     * Returns agent for execution with id [executionId]
     *
     * @param executionId id of execution
     * @return agent
     */
    @Suppress("TYPE_ALIAS")
    fun getAgentIdsForExecution(executionId: Long): Mono<List<String>> = webClientBackend
        .get()
        .uri("/getAgentsIdsForExecution?executionId=$executionId")
        .retrieve()
        .bodyToMono()

    /**
     * Get list of agent ids (containerIds) for agents that have completed their jobs.
     *
     * @param agentId containerId of an agent
     * @return Mono with list of agent ids for agents that can be shut down.
     */
    @Suppress("TYPE_ALIAS")
    fun getAgentsAwaitingStop(agentId: String): Mono<Pair<Long, List<String>>> {
        // If we call this method, then there are no unfinished TestExecutions.
        // check other agents status
        return webClientBackend
            .get()
            .uri("/getAgentsStatusesForSameExecution?agentId=$agentId")
            .retrieve()
            .bodyToMono<AgentStatusesForExecution>()
            .map { (executionId, agentStatuses) ->
                log.debug("For executionId=$executionId agent statuses are $agentStatuses")
                executionId to if (agentStatuses.areIdleOrFinished()) {
                    // We assume, that all agents will eventually have one of these statuses.
                    // Situations when agent gets stuck with a different status and for whatever reason is unable to update
                    // it, are not handled. Anyway, such agents should be eventually stopped: https://github.com/diktat-static-analysis/save-cloud/issues/208
                    agentStatuses.map { it.containerId }
                } else {
                    emptyList()
                }
            }
    }

    /**
     * Perform two operations in arbitrary order: assign `agentContainerId` agent to test executions
     * and mark this agent as BUSY
     *
     * @param agentContainerId id of an agent that receives tests
     * @param newJobResponse a heartbeat response with tests
     */
    internal fun updateAssignedAgent(agentContainerId: String, newJobResponse: NewJobResponse) {
        updateTestExecutionsWithAgent(agentContainerId, newJobResponse.tests).zipWith(
            updateAgentStatusesWithDto(listOf(
                AgentStatusDto(LocalDateTime.now(), AgentState.BUSY, agentContainerId)
            ))
        )
            .doOnSuccess {
                log.debug("Agent $agentContainerId has been set as executor for tests ${newJobResponse.tests} and its status has been set to BUSY")
            }
            .subscribeOn(scheduler)
            .subscribe()
    }

    private fun updateTestExecutionsWithAgent(agentId: String, testDtos: List<TestDto>): Mono<BodilessResponseEntity> {
        log.debug("Attempt to update test executions for tests=$testDtos for agent $agentId")
        return webClientBackend.post()
            .uri("/testExecution/assignAgent?agentContainerId=$agentId")
            .bodyValue(testDtos)
            .retrieve()
            .toBodilessEntity()
    }

    private fun TestBatch.toHeartbeatResponse(agentId: String) =
            if (tests.isNotEmpty()) {
                // fixme: do we still need suitesToArgs, since we have execFlags in save.toml?
                NewJobResponse(
                    tests,
                    suitesToArgs.values.first() +
                            " " + tests.joinToString(separator = " ") { it.filePath }
                )
            } else {
                log.info("Next test batch for agentId=$agentId is empty, setting it to wait")
                WaitResponse
            }

    private fun Collection<AgentStatusDto>.areIdleOrFinished() = all {
        it.state == AgentState.IDLE || it.state == AgentState.FINISHED || it.state == AgentState.STOPPED_BY_ORCH
    }

    companion object {
        private val log = Loggers.getLogger(AgentService::class.java)
    }
}
