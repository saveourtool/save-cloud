package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.backend.repository.AgentStatusRepository
import org.cqfn.save.entities.Agent
import org.cqfn.save.entities.AgentStatus
import org.cqfn.save.entities.AgentStatusDto
import org.cqfn.save.entities.Execution
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * Controller to manipulate with Agent related data
 */
@RestController
class AgentsController(private val agentStatusRepository: AgentStatusRepository,
                       private val agentRepository: AgentRepository,
) {
    /**
     * @param agents list of [Agent]s to save into the DB
     */
    @PostMapping("/addAgents")
    fun addAgents(@RequestBody agents: List<Agent>) {
        agentRepository.saveAll(agents)
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
        agentStates.forEach { dto ->
            val agentStatus = agentStatusRepository.findTopByAgentContainerIdOrderByEndTimeDesc(dto.containerId)
            if (agentStatus != null && agentStatus.state == dto.state) {
                // updating time
                agentStatus.endTime = dto.time
                agentStatusRepository.save(agentStatus)
            } else {
                // insert new agent status
                val agent = getAgentByContainerId(dto.containerId)
                agentStatusRepository.save(AgentStatus(LocalDateTime.now(), dto.time, dto.state, agent))
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
    fun findAllAgentStatusesForSameExecution(@RequestBody agentId: String): List<AgentStatusDto?> =
            agentRepository.findAll { root, cq, cb ->
                cb.equal(root.get<Execution>("execution"), getAgentByContainerId(agentId).execution)
            }.map {
                agentStatusRepository.findTopByAgentContainerIdOrderByEndTimeDesc(it.containerId)?.toDto()
            }

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
}
