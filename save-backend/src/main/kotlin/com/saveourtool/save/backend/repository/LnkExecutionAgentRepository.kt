package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.LnkExecutionAgent
import com.saveourtool.save.spring.repository.BaseEntityRepository

/**
 * Repository of [LnkExecutionAgent]
 */
interface LnkExecutionAgentRepository : BaseEntityRepository<LnkExecutionAgent> {
    /**
     * @param agentId ID of [com.saveourtool.save.entities.Agent]
     * @return [LnkExecutionAgent] or null
     */
    fun findByAgentId(agentId: Long): LnkExecutionAgent?

    /**
     * @param executionId ID of [com.saveourtool.save.entities.Execution]
     * @return list of [LnkExecutionAgent]
     */
    fun findByExecutionId(executionId: Long): List<LnkExecutionAgent>

    /**
     * @param executionIds list of ID of [com.saveourtool.save.entities.Execution]
     * @return list of [LnkExecutionAgent]
     */
    fun findByExecutionIdIn(executionIds: List<Long>): List<LnkExecutionAgent>
}
