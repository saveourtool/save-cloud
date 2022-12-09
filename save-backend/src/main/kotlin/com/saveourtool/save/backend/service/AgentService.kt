package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.AgentRepository
import com.saveourtool.save.backend.repository.AgentStatusRepository
import com.saveourtool.save.backend.repository.LnkExecutionAgentRepository
import com.saveourtool.save.entities.Agent
import com.saveourtool.save.entities.AgentStatus
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.utils.orNotFound
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Service for agent
 */
@Service
class AgentService(
    private val agentRepository: AgentRepository,
    private val agentStatusRepository: AgentStatusRepository,
    private val lnkExecutionAgentRepository: LnkExecutionAgentRepository,
) {
    /**
     * @param executionIds list of ids
     * @return Unit
     */
    @Transactional
    internal fun deleteAgentByExecutionIds(executionIds: List<Long>) = lnkExecutionAgentRepository.findByExecutionIdIn(executionIds)
        .map { it.agent }
        .let {
            agentRepository.deleteAll(it)
        }

    /**
     * @param executionId
     * @return list of [Agent]
     */
    internal fun getAgentsByExecutionId(executionId: Long): List<Agent> = lnkExecutionAgentRepository.findByExecutionId(executionId)
        .map { it.agent }

    /**
     * @param containerId
     * @return [Agent]
     */
    internal fun getAgentByContainerId(containerId: String): Agent = agentRepository.findByContainerId(containerId)
        .orNotFound {
            "Not found agent with container id $containerId"
        }

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

    /**
     * @param agent
     * @return [Execution] to which provided [Agent] assigned
     */
    internal fun getExecution(agent: Agent): Execution = agent.requiredId()
        .let { agentId ->
            lnkExecutionAgentRepository.findByAgentId(agentId)
                .orNotFound { "Not found link to execution for agent $agentId" }
        }
        .execution

    /**
     * @param agentId
     * @return [Execution] to which [Agent] with [agentId] is assigned
     */
    internal fun getExecution(agentId: Long): Execution = agentRepository.findByIdOrNull(agentId)
        .orNotFound {
            "Not found agent with id $agentId"
        }
        .let {
            getExecution(it)
        }

    /**
     * @param containerId
     * @return [Execution] to which [Agent] with [containerId] is assigned
     */
    internal fun getExecutionByContainerId(containerId: String): Execution = getExecution(getAgentByContainerId(containerId))
}
