/**
 * JPA repositories for Agent related data
 */

package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Agent
import org.cqfn.save.entities.AgentStatus
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
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
    fun findTopByAgentContainerIdOrderByEndTimeDesc(containerId: String): AgentStatus?
}

/**
 * JPA repository for agents.
 */
@Repository
interface AgentRepository : BaseEntityRepository<Agent>, JpaSpecificationExecutor<Agent>
