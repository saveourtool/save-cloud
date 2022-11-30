package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.AgentStatus
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

/**
 * JPA repository for agent statuses.
 */
@Repository
interface AgentStatusRepository : BaseEntityRepository<AgentStatus> {
    /**
     * Find latest [AgentStatus] for agent with provided [containerId]
     *
     * @param containerId id of an agent
     * @return [AgentStatus] of an agent
     */
    fun findTopByAgentContainerIdOrderByEndTimeDescIdDesc(containerId: String): AgentStatus?

    /**
     * Find all agent statuses with [projectId] in execution
     *
     * @param projectId id of project
     * @return [AgentStatus] of an agent
     */
    fun findByAgentExecutionProjectId(projectId: Long): List<AgentStatus>

    /**
     * @param ids list of executions id
     */
    @Transactional
    fun deleteByAgentExecutionIdIn(ids: List<Long>)
}
