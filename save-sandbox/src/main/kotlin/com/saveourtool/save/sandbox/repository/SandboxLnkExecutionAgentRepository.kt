package com.saveourtool.save.sandbox.repository

import com.saveourtool.save.sandbox.entity.SandboxLnkExecutionAgent
import com.saveourtool.save.spring.repository.BaseEntityRepository

/**
 * Repository of [SandboxLnkExecutionAgent]
 */
interface SandboxLnkExecutionAgentRepository : BaseEntityRepository<SandboxLnkExecutionAgent> {
    /**
     * @param agentId ID of [com.saveourtool.save.entities.Agent]
     * @return [SandboxLnkExecutionAgent] or null
     */
    fun findByAgentId(agentId: Long): SandboxLnkExecutionAgent?

    /**
     * @param executionId ID of [com.saveourtool.save.sandbox.entity.SandboxExecution]
     * @return list of [SandboxLnkExecutionAgent]
     */
    fun findByExecutionId(executionId: Long): List<SandboxLnkExecutionAgent>
}
