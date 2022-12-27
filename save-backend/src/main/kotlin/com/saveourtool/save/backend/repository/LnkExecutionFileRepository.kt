package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.LnkExecutionFile
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository of [LnkExecutionFile]
 */
@Repository
interface LnkExecutionFileRepository : BaseEntityRepository<LnkExecutionFile> {
    /**
     * @param execution execution that is connected to [com.saveourtool.save.entities.File]
     * @return [LnkExecutionFile] by [execution]
     */
    fun findByExecution(execution: Execution): List<LnkExecutionFile>

    /**
     * @param executionId id of [Execution] that is connected to [com.saveourtool.save.entities.File]
     * @return [LnkExecutionFile] by [executionId]
     */
    fun findByExecutionId(executionId: Long): List<LnkExecutionFile>
}
