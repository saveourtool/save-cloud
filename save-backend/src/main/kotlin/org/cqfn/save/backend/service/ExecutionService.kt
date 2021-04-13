package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.BaseEntityRepository
import org.cqfn.save.entities.Execution
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.execution.ExecutionUpdateDto
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

/**
 * Service that is used to manipulate executions
 */
@Service
class ExecutionService(private val baseEntityRepository: BaseEntityRepository<Execution>) {
    /**
     * @param execution
     * @return id of the created [Execution]
     */
    fun saveExecution(execution: Execution): Long = baseEntityRepository.save(execution).id!!

    /**
     * @param execution
     * @throws ResponseStatusException
     */
    fun updateExecution(execution: ExecutionUpdateDto) {
        baseEntityRepository.findById(execution.id).ifPresentOrElse({
            if (it.status == ExecutionStatus.FINISHED) {
                it.endTime = LocalDateTime.now()
            }
            it.status = execution.status
            baseEntityRepository.save(it)
        }) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
    }
}
