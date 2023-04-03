package com.saveourtool.save.sandbox.repository

import com.saveourtool.save.entities.Agent
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [Agent]
 */
@Repository
interface SandboxAgentRepository : BaseEntityRepository<Agent> {
    /**
     * Find agent by its container id
     *
     * @param containerId
     * @return [Agent]
     */
    fun findByContainerId(containerId: String): Agent?
}
