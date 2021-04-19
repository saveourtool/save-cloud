/**
 * JPA repositories for Agent related data
 */

package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Agent
import org.cqfn.save.entities.AgentStatus
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

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
    fun findTopByAgentContainerIdOrderByTimeDesc(containerId: String): AgentStatus?
}

/**
 * JPA repository for agents.
 */
@Repository
interface AgentRepository : BaseEntityRepository<Agent>, JpaSpecificationExecutor<Agent> {
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
    fun findByContainerId(containerId: String): Agent?

    /**
     * Find all agents that have the same `executionId` as the agent with provided [containerId].
     *
     * @param containerId containerId of an agent
     * @return list of agents from the same Execution
     */
    @Query(value = "SELECT * FROM agent WHERE agent.execution_id = (SELECT execution_id FROM agent WHERE agent.container_id = ?1)", nativeQuery = true)
    fun findByExecutionIdOfContainerId(containerId: String): List<Agent>
}
