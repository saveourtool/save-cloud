package com.saveourtool.save.sandbox.repository

import com.saveourtool.save.sandbox.entity.SandboxExecution
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [SandboxExecution]
 */
@Repository
interface SandboxExecutionRepository : BaseEntityRepository<SandboxExecution> {
    /**
     * @param userId
     * @return list of [SandboxExecution] for requested [userId]
     */
    fun findByUserId(userId: Long): List<SandboxExecution>

    /**
     * @param userId
     * @return latest [SandboxExecution] for requested [userId]
     */
    fun findTopByUserIdOrderByStartTimeDesc(userId: Long): SandboxExecution?
}
