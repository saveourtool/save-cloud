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
    fun findTopByAgentContainerIdOrderByTimeDesc(containerId: String): AgentStatus
}

/**
 * JPA repository for agents.
 */
@Repository
interface AgentRepository : BaseEntityRepository<Agent> {
    fun findByExecutionId(executionId: Long): List<Agent>

    fun findByContainerId(containerId: String): List<Agent>
}
