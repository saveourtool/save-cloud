package com.saveourtool.save.backend.controllers.internal

import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.agent.AgentVersion
import com.saveourtool.save.backend.repository.AgentRepository
import com.saveourtool.save.backend.repository.AgentStatusRepository
import com.saveourtool.save.entities.Agent
import com.saveourtool.save.entities.AgentStatus
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.entities.AgentStatusesForExecution
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.kotlin.core.publisher.toMono

/**
 * Controller to manipulate with Agent related data
 */
@RestController
@RequestMapping("/internal")
class AgentsController(private val agentStatusRepository: AgentStatusRepository,
                       private val agentRepository: AgentRepository,
) {
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
     * @param agentStates list of [AgentStatus]es to update in the DB
     */
    @PostMapping("/updateAgentStatusesWithDto")
    @Transactional
    fun updateAgentStatusesWithDto(@RequestBody agentState: AgentStatusDto) {
        val agentStatus = agentStatusRepository.findTopByAgentContainerIdOrderByEndTimeDescIdDesc(agentState.containerId)
        if (agentStatus != null) {
            val latestState = agentStatus.state
            if (latestState == AgentState.STOPPED_BY_ORCH || latestState == AgentState.TERMINATED) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Agent ${agentState.containerId} has state $latestState and shouldn't be updated")
            } else if (agentStatus.state == agentState.state) {
                // updating time
                agentStatus.endTime = agentState.time
                agentStatusRepository.save(agentStatus)
            }
        } else {
            // insert new agent status
            val agent = getAgentByContainerId(agentState.containerId)
            agentStatusRepository.save(AgentStatus(agentState.time, agentState.time, agentState.state, agent))
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
     * Return ID of execution for which agent [agentId] has been created
     *
     * @param agentId id of an agent to look for
     * @return id of an execution
     */
    @GetMapping("/agents/{agentId}/execution/id")
    fun findExecutionIdByAgentId(@PathVariable agentId: String) = agentRepository.findByContainerId(agentId)
        .toMono()
        .mapNotNull { it.execution.id }

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
