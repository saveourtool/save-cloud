package org.cqfn.save.backend.controllers.internal

import org.cqfn.save.agent.AgentVersion
import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.backend.repository.AgentStatusRepository
import org.cqfn.save.entities.Agent
import org.cqfn.save.entities.AgentStatus
import org.cqfn.save.entities.AgentStatusDto
import org.cqfn.save.entities.AgentStatusesForExecution
import org.cqfn.save.currentVersion
import org.cqfn.save.v2_0
import org.cqfn.save.v1_0
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
    @PostMapping(path = ["/${v1_0}/addAgents", "/${currentVersion}/addAgents"])
    @Suppress("UnsafeCallOnNullableType")  // hibernate should always assign ids
    fun addAgents(@RequestBody agents: List<Agent>): List<Long> {
        log.debug("Saving agents $agents")
        return agentRepository.saveAll(agents).map { it.id!! }
    }

    /**
     * @param agentStates list of [AgentStatus]es to update in the DB
     */
    @PostMapping(path = ["/${v2_0}/updateAgentStatuses", "/${currentVersion}/updateAgentStatuses"])
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
    fun updateAgentStatusesWithDto(@RequestBody agentStates: List<AgentStatusDto>) {
        agentStates.forEach { dto ->
            val agentStatus = agentStatusRepository.findTopByAgentContainerIdOrderByEndTimeDescIdDesc(dto.containerId)
            if (agentStatus != null && agentStatus.state == dto.state) {
                // updating time
                agentStatus.endTime = dto.time
                agentStatusRepository.save(agentStatus)
            } else {
                // insert new agent status
                val agent = getAgentByContainerId(dto.containerId)
                agentStatusRepository.save(AgentStatus(dto.time, dto.time, dto.state, agent))
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
        return agent.orElseThrow { IllegalStateException("Agent with containerId=$containerId not found in the DB") }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AgentsController::class.java)
    }
}
