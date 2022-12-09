package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.AgentStatusRepository
import com.saveourtool.save.backend.repository.LnkExecutionAgentRepository
import org.springframework.stereotype.Service

/**
 * Service for agent status
 */
@Service
class AgentStatusService(
    private val agentStatusRepository: AgentStatusRepository,
    private val lnkExecutionAgentRepository: LnkExecutionAgentRepository,
) {
    /**
     * @param executionIds
     * @return Unit
     */
    internal fun deleteAgentStatusWithExecutionIds(executionIds: List<Long>) = lnkExecutionAgentRepository.findByExecutionIdIn(executionIds)
        .map { it.agent.requiredId() }
        .let { agentStatusRepository.deleteByAgentIdIn(it) }
}
