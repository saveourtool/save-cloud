package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.Project
import org.springframework.stereotype.Service

/**
 * Service for agent
 */
@Service
class AgentService(private val agentRepository: AgentRepository) {
    /**
     * @param projectId
     */
    internal fun deleteAgentWithProjectId(projectId: Long?) {
        val agents = agentRepository.findAll { root, _, cb ->
            cb.equal(root.get<Execution>("execution").get<Project>("project").get<Long>("id"), projectId)
        }

        agents.forEach {
            agentRepository.delete(it)
        }
    }
}
