package com.saveourtool.save.backend.controllers.internal

import com.saveourtool.save.agent.AgentInitConfig
import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.agent.AgentVersion
import com.saveourtool.save.agent.SaveCliOverrides
import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.AgentRepository
import com.saveourtool.save.backend.repository.AgentStatusRepository
import com.saveourtool.save.entities.*
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.switchIfEmptyToNotFound
import generated.SAVE_CORE_VERSION
import org.hibernate.Hibernate
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

/**
 * Controller to manipulate with Agent related data
 */
@RestController
@RequestMapping("/internal")
class AgentsController(
    private val agentStatusRepository: AgentStatusRepository,
    private val agentRepository: AgentRepository,
    private val configProperties: ConfigProperties,
) {
    /**
     * @param containerId [Agent.containerId]
     * @return [Mono] with [AgentInitConfig]
     */
    @GetMapping("/agents/get-init-config")
    fun getInitConfig(
        @RequestParam containerId: String,
    ): Mono<AgentInitConfig> = blockingToMono {
        agentRepository.findByContainerId(containerId)
    }
        .switchIfEmptyToNotFound {
            "Not found agent with container id $containerId"
        }
        .map {
            it.execution
        }
        .map { execution ->
            AgentInitConfig(
                saveCliUrl = "${configProperties.url}/internal/files/download-save-cli?version=$SAVE_CORE_VERSION",
                testSuitesSourceSnapshotUrl = "${configProperties.url}/internal/test-suites-sources/download-snapshot-by-execution-id?executionId=${execution.requiredId()}",
                additionalFileNameToUrl = execution.parseAndGetAdditionalFiles()
                    .associate {
                        it.name to "${configProperties.url}/internal/files/download?name=${it.name}&uploadedMillis=${it.uploadedMillis}&executionId=${execution.requiredId()}"
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
     * @param agents list of [Agent]s to save into the DB
     * @return a list of IDs, assigned to the agents
     */
    @PostMapping("/addAgents")
    @Suppress("UnsafeCallOnNullableType")  // hibernate should always assign ids
    fun addAgents(@RequestBody agents: List<Agent>): List<Long> {
        log.debug("Saving agents $agents")
        return agentRepository.saveAll(agents).map { it.id!! }
    }

    /**
     * @param agentStates list of [AgentStatus]es to update in the DB
     */
    @PostMapping("/updateAgentStatuses")
    fun updateAgentStatuses(@RequestBody agentStates: List<AgentStatus>) {
        agentStatusRepository.saveAll(agentStates)
    }

    /**
     * @param agentStates list of [AgentStatus]es to update in the DB
     */
    @PostMapping("/updateAgentStatusesWithDto")
    @Transactional
    fun updateAgentStatusesWithDto(@RequestBody agentStates: List<AgentStatusDto>) {
        agentStates.forEach {
            updateAgentStatusWithDto(it)
        }
    }

    /**
     * @param agentVersion [AgentVersion] to update agent version
     */
    @PostMapping("/saveAgentVersion")
    fun updateAgentVersion(@RequestBody agentVersion: AgentVersion) {
        agentRepository.findByContainerId(agentVersion.containerId)?.let {
            it.version = agentVersion.version
            agentRepository.save(it)
        }
    }

    /**
     * @param agentState an [AgentStatus] to update in the DB
     * @throws ResponseStatusException code 409 if agent has already its final state that shouldn't be updated
     */
    @PostMapping("/updateAgentStatusWithDto")
    @Transactional
    fun updateAgentStatusWithDto(@RequestBody agentState: AgentStatusDto) {
        val agentStatus = agentStatusRepository.findTopByAgentContainerIdOrderByEndTimeDescIdDesc(agentState.containerId)
        when (val latestState = agentStatus?.state) {
            AgentState.STOPPED_BY_ORCH, AgentState.TERMINATED ->
                throw ResponseStatusException(HttpStatus.CONFLICT, "Agent ${agentState.containerId} has state $latestState and shouldn't be updated")
            agentState.state -> {
                // updating time
                agentStatus.endTime = agentState.time
                agentStatusRepository.save(agentStatus)
            }
            else -> {
                // insert new agent status
                agentStatusRepository.save(agentState.toEntity { getAgentByContainerId(it) })
            }
        }
    }

    /**
     * Get statuses of all agents in the same execution with provided agent (including itself).
     *
     * @param agentId containerId of an agent.
     * @return list of agent statuses
     * @throws IllegalStateException if provided [agentId] is invalid.
     */
    @GetMapping("/getAgentsStatusesForSameExecution")
    @Transactional
    @Suppress("UnsafeCallOnNullableType")  // id will be available because it's retrieved from DB
    fun findAllAgentStatusesForSameExecution(@RequestParam agentId: String): AgentStatusesForExecution {
        val execution = getAgentByContainerId(agentId).execution
        val agentStatuses = agentRepository.findByExecutionId(execution.id!!).map { agent ->
            val latestStatus = requireNotNull(
                agentStatusRepository.findTopByAgentContainerIdOrderByEndTimeDescIdDesc(agent.containerId)
            ) {
                "AgentStatus not found for agent id=${agent.containerId}"
            }
            latestStatus.toDto()
        }
        return AgentStatusesForExecution(execution.id!!, agentStatuses)
    }

    /**
     * Get statuses of agents identified by [agentIds].
     *
     * @param agentIds a list of containerIds agents.
     * @return list of agent statuses
     */
    @GetMapping("/agents/statuses")
    @Transactional
    fun findAgentStatuses(@RequestParam(name = "ids") agentIds: List<String>): List<AgentStatusDto> {
        val agents = agentIds.map { agentId ->
            val agent = agentRepository.findByContainerId(agentId)
            requireNotNull(agent) {
                "Agent in the DB not found for containerId=$agentId"
            }
        }
        check(agents.distinctBy { it.execution.id }.size == 1) {
            "Statuses are requested for agents from different executions: agentIds=$agentIds, execution IDs are ${agents.map { it.execution.id }}"
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
     * Returns containerIds for all agents for [executionId]
     *
     * @param executionId id of execution
     * @return list of container ids
     */
    @GetMapping("/getAgentsIdsForExecution")
    fun findAgentIdsForExecution(@RequestParam executionId: Long) = agentRepository.findByExecutionId(executionId)
        .map(Agent::containerId)

    /**
     * Get agent by containerId.
     *
     * @param containerId containerId of an agent.
     * @return list of agent statuses
     */
    private fun getAgentByContainerId(containerId: String): Agent {
        val agent = agentRepository.findOne { root, _, cb ->
            cb.equal(root.get<String>("containerId"), containerId)
        }
        return agent.orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Agent with containerId=$containerId not found in the DB") }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AgentsController::class.java)
    }
}
