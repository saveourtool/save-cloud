/**
 * JPA repositories for Agent related data
 */

package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Agent
import org.cqfn.save.entities.AgentStatus
import org.springframework.data.jpa.repository.JpaRepository

/**
 * JPA repository for agent statuses.
 */
interface AgentStatusRepository : JpaRepository<AgentStatus, Long>

/**
 * JPA repository for agents.
 */
interface AgentRepository : JpaRepository<Agent, String>
