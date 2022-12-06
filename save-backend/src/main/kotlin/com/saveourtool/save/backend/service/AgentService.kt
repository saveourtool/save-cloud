package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.AgentRepository
import com.saveourtool.save.backend.repository.AgentStatusRepository
import com.saveourtool.save.entities.Agent
import com.saveourtool.save.entities.AgentStatus
import com.saveourtool.save.utils.orNotFound
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Service for agent
 */
@Service
class AgentService(
    private val agentRepository: AgentRepository,
    private val agentStatusRepository: AgentStatusRepository,
) {
    /**
     * @param projectId
     * @return Unit
     */
    internal fun deleteAgentWithProjectId(projectId: Long) =
            agentRepository.findByExecutionProjectId(projectId).forEach {
                agentRepository.delete(it)
            }

    /**
     * @param executionIds list of ids
     * @return Unit
     */
    internal fun deleteAgentByExecutionIds(executionIds: List<Long>) =
            agentRepository.deleteByExecutionIdIn(executionIds)

    /**
     * @param executionId
     * @return list of [Agent]
     */
    internal fun getAgentsByExecutionId(executionId: Long): List<Agent> = agentRepository.findByExecutionId(executionId)

    /**
     * @param containerName
     * @return [Agent]
     */
    internal fun getAgentByContainerName(containerName: String): Agent = agentRepository.findByContainerName(containerName)
        .orNotFound {
            "Not found agent with container name $containerName"
        }

    /**
     * @param agent
     * @return first [AgentStatus.startTime]  and last [AgentStatus.endTime]
     */
    internal fun getAgentTimes(agent: Agent): Pair<LocalDateTime, LocalDateTime> {
        val startTime = agentStatusRepository.findTopByAgentOrderByStartTimeAsc(agent)?.startTime
            .orNotFound {
                "Not found first agent status for agent id ${agent.requiredId()}"
            }
        val endTime = agentStatusRepository.findTopByAgentOrderByEndTimeDesc(agent)?.endTime
            .orNotFound {
                "Not found last agent status for agent id ${agent.requiredId()}"
            }
        return startTime to endTime
    }
}
