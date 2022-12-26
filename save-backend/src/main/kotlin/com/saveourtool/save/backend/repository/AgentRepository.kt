package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.Agent
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * JPA repository for agents.
 */
@Repository
interface AgentRepository : BaseEntityRepository<Agent> {
    /**
     * Find agent by its container id
     *
     * @param containerId container id
     * @return [Agent]
     */
    fun findByContainerId(containerId: String): Agent?

    /**
     * Find agent by its container name
     *
     * @param containerName
     * @return [Agent]
     */
    fun findByContainerName(containerName: String): Agent?
}
