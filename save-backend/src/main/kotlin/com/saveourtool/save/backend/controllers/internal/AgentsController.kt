package com.saveourtool.save.backend.controllers.internal

import com.saveourtool.save.agent.*
import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.AgentStatusRepository
import com.saveourtool.save.backend.service.AgentService
import com.saveourtool.save.backend.service.TestExecutionService
import com.saveourtool.save.backend.service.TestService
import com.saveourtool.save.entities.*
import com.saveourtool.save.test.TestDto
import com.saveourtool.save.utils.*

import generated.SAVE_CORE_VERSION
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

import kotlinx.datetime.toJavaLocalDateTime

/**
 * Controller to manipulate with Agent related data
 */
@RestController
@RequestMapping("/internal")
class AgentsController(
    private val agentStatusRepository: AgentStatusRepository,
    private val agentService: AgentService,
    private val configProperties: ConfigProperties,
    private val testService: TestService,
    private val testExecutionService: TestExecutionService,
) {
    /**
     * @param containerId [Agent.containerId]
     * @return [Mono] with [AgentInitConfig]
     */
    @GetMapping("/agents/get-init-config")
    fun getInitConfig(
        @RequestParam containerId: String,
    ): Mono<AgentInitConfig> = getAgentByContainerIdAsMono(containerId)
        .map {
            agentService.getExecution(it)
        }
        .map { execution ->
            val backendUrl = configProperties.agentSettings.backendUrl

            AgentInitConfig(
                saveCliUrl = "$backendUrl/internal/files/download-save-cli?version=$SAVE_CORE_VERSION",
                testSuitesSourceSnapshotUrl = "$backendUrl/internal/test-suites-sources/download-snapshot-by-execution-id?executionId=${execution.requiredId()}",
                additionalFileNameToUrl = execution.getFileKeys()
                    .associate { fileKey ->
                        fileKey.name to buildString {
                            append(backendUrl)
                            append("/internal/files/download?")
                            mapOf(
                                "organizationName" to fileKey.projectCoordinates.organizationName,
                                "projectName" to fileKey.projectCoordinates.projectName,
                                "name" to fileKey.name,
                                "uploadedMillis" to fileKey.uploadedMillis,
                            )
                                .map { (key, value) -> "$key=$value" }
                                .joinToString("&")
                                .let { append(it) }
                        }
                    },
                saveCliOverrides = SaveCliOverrides(
                    overrideExecCmd = execution.execCmd,
                    overrideExecFlags = null,
                    batchSize = execution.batchSizeForAnalyzer?.takeIf { it.isNotBlank() }?.toInt(),
                    batchSeparator = null,
                ),
            )
        }

    /**
     * @param containerId [Agent.containerId]
     * @return [Mono] with [AgentRunConfig]
     */
    @GetMapping("/agents/get-next-run-config")
    @Transactional
    fun getNextRunConfig(
        @RequestParam containerId: String,
    ): Mono<AgentRunConfig> = getAgentByContainerIdAsMono(containerId)
        .map {
            agentService.getExecution(it)
        }
        .zipWhen { execution ->
            testService.getTestBatches(execution)
        }
        .filter { (_, testBatch) -> testBatch.isNotEmpty() }
        .map { (execution, testBatch) ->
            val backendUrl = configProperties.agentSettings.backendUrl

            testBatch to AgentRunConfig(
                cliArgs = testBatch.constructCliCommand(),
                executionDataUploadUrl = "$backendUrl/internal/saveTestResult",
                debugInfoUploadUrl = "$backendUrl/internal/files/debug-info?executionId=${execution.requiredId()}"
            )
        }
        .asyncEffect { (testBatch, _) ->
            blockingToMono { testExecutionService.assignAgentByTest(containerId, testBatch) }
                .doOnSuccess {
                    log.trace { "Agent $containerId has been set as executor for tests $testBatch and its status has been set to BUSY" }
                }
        }
        .map { (_, runConfig) -> runConfig }

    /**
     * @param executionId ID of [Execution]
     * @param agents list of [AgentDto]s to save into the DB
     * @return a list of IDs, assigned to the agents
     */
    @PostMapping("/agents/insert")
    fun addAgents(
        @RequestParam executionId: Long,
        @RequestBody agents: List<AgentDto>,
    ): List<Long> {
        log.debug("Saving agents $agents")
        return agents
            .map { it.toEntity() }
            .let { agentService.saveAll(executionId, it) }
            .map { it.requiredId() }
    }

    /**
     * @param agentStates list of [AgentStatus]es to update in the DB
     * @throws ResponseStatusException code 409 if agent has already its final state that shouldn't be updated
     */
    @PostMapping("/updateAgentStatusesWithDto")
    @Transactional
    fun updateAgentStatusesWithDto(@RequestBody agentStates: List<AgentStatusDto>) {
        agentStates.forEach { agentState ->
            val agentStatus = agentStatusRepository.findTopByAgentContainerIdOrderByEndTimeDescIdDesc(agentState.containerId)
            when (val latestState = agentStatus?.state) {
                AgentState.STOPPED_BY_ORCH, AgentState.TERMINATED ->
                    throw ResponseStatusException(HttpStatus.CONFLICT, "Agent ${agentState.containerId} has state $latestState and shouldn't be updated")
                agentState.state -> {
                    // updating time
                    agentStatus.endTime = agentState.time.toJavaLocalDateTime()
                    agentStatusRepository.save(agentStatus)
                }
                else -> {
                    // insert new agent status
                    agentStatusRepository.save(agentState.toEntity { getAgentByContainerId(it) })
                }
            }
        }
    }

    /**
     * Get statuses of all agents in the same execution with provided agent (including itself).
     *
     * @param containerId containerId of an agent.
     * @return list of agent statuses
     * @throws IllegalStateException if provided [containerId] is invalid.
     */
    @GetMapping("/getAgentsStatusesForSameExecution")
    @Transactional
    @Suppress("UnsafeCallOnNullableType")  // id will be available because it's retrieved from DB
    fun findAllAgentStatusesForSameExecution(@RequestParam("agentId") containerId: String): AgentStatusesForExecution {
        val executionId = agentService.getExecutionByContainerId(containerId).requiredId()
        val agentStatuses = agentService.getAgentsByExecutionId(executionId).map { agent ->
            val latestStatus = requireNotNull(
                agentStatusRepository.findTopByAgentContainerIdOrderByEndTimeDescIdDesc(agent.containerId)
            ) {
                "AgentStatus not found for agent id=${agent.containerId}"
            }
            latestStatus.toDto()
        }
        return AgentStatusesForExecution(executionId, agentStatuses)
    }

    /**
     * Get statuses of agents identified by [containerIds].
     *
     * @param containerIds a list of containerIds agents.
     * @return list of agent statuses
     */
    @GetMapping("/agents/statuses")
    @Transactional
    fun findAgentStatuses(@RequestParam(name = "ids") containerIds: List<String>): List<AgentStatusDto> {
        val agents = containerIds.map {
            agentService.getAgentByContainerId(it)
        }
        val executionIds = agents.map { agentService.getExecution(it).requiredId() }.distinct()
        check(executionIds.size == 1) {
            "Statuses are requested for agents from different executions: agentIds=$containerIds, execution IDs are $executionIds"
        }
        return agents.map { agent ->
            val latestStatus = requireNotNull(
                agentStatusRepository.findTopByAgentContainerIdOrderByEndTimeDescIdDesc(agent.containerId)
            ) {
                "AgentStatus not found for agent id=${agent.containerId}"
            }
            latestStatus.toDto()
        }
    }

    /**
     * Get agent by containerId.
     *
     * @param containerId containerId of an agent.
     * @return [Agent]
     */
    private fun getAgentByContainerId(containerId: String): Agent = agentService.getAgentByContainerId(containerId)

    private fun getAgentByContainerIdAsMono(containerId: String): Mono<Agent> = blockingToMono {
        getAgentByContainerId(containerId)
    }

    private fun List<TestDto>.constructCliCommand() = joinToString(" ") { it.filePath }
        .also {
            log.debug("Constructed cli args for SAVE-cli: $it")
        }

    companion object {
        private val log = LoggerFactory.getLogger(AgentsController::class.java)
    }
}
