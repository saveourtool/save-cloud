package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.Agent
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

/**
 * JPA repository for agents.
 */
@Repository
interface AgentRepository : BaseEntityRepository<Agent> {
    /**
     * Find agent by its agent id
     *
     * @param agentId agent id
     * @return [Agent]
     */
    fun findByContainerId(agentId: String): Agent?

    /**
     * Find agent by its container name
     *
     * @param containerName
     * @return [Agent]
     */
    fun findByContainerName(containerName: String): Agent?

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