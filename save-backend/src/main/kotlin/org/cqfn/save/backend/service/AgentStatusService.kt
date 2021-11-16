package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.AgentStatusRepository
import org.springframework.stereotype.Service

/**
 * Service for agent status
 */
@Service
class AgentStatusService(private val agentStatusRepository: AgentStatusRepository) {
    /**
     * @param projectId
     * @return Unite
     */
    internal fun deleteAgentStatusWithProjectId(projectId: Long) =
            agentStatusRepository.findByAgentExecutionProjectId(projectId).forEach {
                agentStatusRepository.delete(it)
            }

    /**
     * @param executionIds
     * @return Unite
     */
    internal fun deleteAgentStatusWithExecutionIds(executionIds: List<Long>) =
            agentStatusRepository.deleteByAgentExecutionIdIn(executionIds)
}
