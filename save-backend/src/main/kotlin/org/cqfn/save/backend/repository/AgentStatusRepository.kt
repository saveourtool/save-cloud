/**
 * JPA repositories for Agent related data
 */

package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Agent
import org.cqfn.save.entities.AgentStatus
import org.springframework.stereotype.Repository

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
    fun findTopByAgentContainerIdOrderByTimeDesc(containerId: String): AgentStatus
}

/**
 * JPA repository for agents.
 */
@Repository
interface AgentRepository : BaseEntityRepository<Agent> {
    /**
     * Find agents by execution id
     *
     * @param executionId id of and Execution
     * @return list of agents
     */
    fun findByExecutionId(executionId: Long): List<Agent>

    /**
     * Find agents by containerId
     *
     * @param containerId id of a container
     * @return list of agents
     */
    fun findByContainerId(containerId: String): List<Agent>
}
