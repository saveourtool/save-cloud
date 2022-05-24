package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.AgentStatusRepository
import org.springframework.stereotype.Service

/**
 * Service for agent status
 */
@Service
class AgentStatusService(private val agentStatusRepository: AgentStatusRepository) {
    /**
     * @param projectId
     * @return Unit
     */
    internal fun deleteAgentStatusWithProjectId(projectId: Long) =
            agentStatusRepository.findByAgentExecutionProjectId(projectId).forEach {
                agentStatusRepository.delete(it)
            }

    /**
     * @param executionIds
     * @return Unit
     */
    internal fun deleteAgentStatusWithExecutionIds(executionIds: List<Long>) =
            agentStatusRepository.deleteByAgentExecutionIdIn(executionIds)
}
