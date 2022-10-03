package com.saveourtool.save.sandbox.repository

import com.saveourtool.save.entities.AgentStatus
import com.saveourtool.save.sandbox.entity.SandboxAgentStatus
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [SandboxAgentStatus]
 */
@Repository
interface SandboxAgentStatusRepository : BaseEntityRepository<SandboxAgentStatus> {
    /**
     * Find latest [SandboxAgentStatus] for agent with provided [containerId]
     *
     * @param containerId id of an agent
     * @return [SandboxAgentStatus] of an agent
     */
    fun findTopByAgentContainerIdOrderByEndTimeDescIdDesc(containerId: String): AgentStatus?
}
