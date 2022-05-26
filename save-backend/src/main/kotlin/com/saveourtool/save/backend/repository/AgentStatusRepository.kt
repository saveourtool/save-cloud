/**
 * JPA repositories for Agent related data
 */

package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.Agent
import com.saveourtool.save.entities.AgentStatus
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

/**
 * JPA repository for agent statuses.
 */
@Repository
interface AgentStatusRepository : BaseEntityRepository<AgentStatus>, JpaSpecificationExecutor<AgentStatus> {
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

/**
 * JPA repository for agents.
 */
@Repository
interface AgentRepository : BaseEntityRepository<Agent>, JpaSpecificationExecutor<Agent> {
    /**
     * Find agent by its agent id
     *
     * @param agentId agent id
     * @return [Agent]
     */
    fun findByContainerId(agentId: String): Agent?

    /**
     * Find all agents with [executionId]
     *
     * @param executionId id of execution
     * @return list of agents
     */
    fun findByExecutionId(executionId: Long): List<Agent>

    /**
     * Find all agents with [projectId] in execution
     *
     * @param projectId id of project
     * @return list of agents
     */
    fun findByExecutionProjectId(projectId: Long): List<Agent>

    /**
     * Delete all agents with [executionId]
     *
     * @param ids list id of execution
     */
    @Transactional
    fun deleteByExecutionIdIn(ids: List<Long>)
}
