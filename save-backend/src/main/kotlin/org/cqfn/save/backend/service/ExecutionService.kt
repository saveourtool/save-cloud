package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.entities.Execution
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.execution.ExecutionUpdateDto
import org.springframework.stereotype.Service
import java.time.LocalDateTime

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

    /**
     * @param execution
     */
    fun updateExecution(execution: ExecutionUpdateDto) {
        val databaseExecution = executionRepository.findById(execution.id).ifPresent {
            if (it.status == ExecutionStatus.FINISHED) {
                it.endTime = LocalDateTime.now()
            }
            it.status = execution.status
            executionRepository.save(it)
        }
    }
}
