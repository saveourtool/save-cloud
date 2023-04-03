package com.saveourtool.save.sandbox.repository

import com.saveourtool.save.entities.Agent
import com.saveourtool.save.entities.AgentStatus
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [AgentStatus] in sandbox
 */
@Repository
interface SandboxAgentStatusRepository : BaseEntityRepository<AgentStatus> {
    /**
     * Find latest [AgentStatus] for agent with provided [containerId]
     *
     * @param containerId id of an agent
     * @return [AgentStatus] of an agent
     */
    fun findTopByAgentContainerIdOrderByEndTimeDescIdDesc(containerId: String): AgentStatus?

    /**
     * Find [AgentStatus] by [Agent] which is first by [AgentStatus.startTime]
     *
     * @param agent
     * @return [AgentStatus] which fits to query
     */
    fun findTopByAgentOrderByStartTimeAsc(agent: Agent): AgentStatus?

    /**
     * Find [AgentStatus] by [Agent] which is last by [AgentStatus.endTime]
     *
     * @param agent
     * @return [AgentStatus] which fits to query
     */
    fun findTopByAgentOrderByEndTimeDesc(agent: Agent): AgentStatus?
}
