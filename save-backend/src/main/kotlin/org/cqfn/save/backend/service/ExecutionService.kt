package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.entities.Execution
import org.springframework.stereotype.Service

/**
 * Service that is used to manipulate executions
 */
@Service
class ExecutionService(private val executionRepository: ExecutionRepository) {
    /**
     * @param execution
     */
    fun saveExecution(execution: Execution) {
        executionRepository.save(execution)
    }
}
