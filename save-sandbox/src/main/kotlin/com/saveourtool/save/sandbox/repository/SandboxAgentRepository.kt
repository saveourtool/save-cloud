package com.saveourtool.save.sandbox.repository

import com.saveourtool.save.sandbox.entity.SandboxAgent
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

@Repository
interface SandboxAgentRepository : BaseEntityRepository<SandboxAgent> {
    /**
     * Find agent by its container id
     *
     * @param containerId
     * @return [SandboxAgent]
     */
    fun findByContainerId(containerId: String): SandboxAgent?

    /**
     * Find all agents with [executionId]
     *
     * @param executionId id of execution
     * @return list of [SandboxAgent]
     */
    fun findByExecutionId(executionId: Long): List<SandboxAgent>
}
