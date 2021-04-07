package org.cqfn.save.backend.controllers

import org.cqfn.save.agent.AgentState
import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.backend.repository.AgentStatusRepository
import org.cqfn.save.entities.Agent
import org.cqfn.save.entities.AgentStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AgentsController(private val agentStatusRepository: AgentStatusRepository,
                       private val agentRepository: AgentRepository,
) {
    @PostMapping("/addAgents")
    fun addAgents(@RequestBody agents: List<Agent>) {
        agentRepository.saveAll(agents)
    }

    @PostMapping("/updateAgentStatuses")
    fun updateAgentStatuses(@RequestBody agentStates: List<AgentStatus>) {
        agentStatusRepository.saveAll(agentStates)
    }
}
