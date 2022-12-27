package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.File
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
     * @return [LnkExecutionFile] by [Execution]
     */
    fun findAllByExecution(execution: Execution): List<LnkExecutionFile>

    /**
     * @param executionId id of [Execution] that is connected to [com.saveourtool.save.entities.File]
     * @return [LnkExecutionFile] by [Execution.id]
     */
    fun findAllByExecutionId(executionId: Long): List<LnkExecutionFile>

    /**
     * @param file id of [com.saveourtool.save.entities.File] that is connected to [com.saveourtool.save.entities.Execution]
     * @return [LnkExecutionFile] by [com.saveourtool.save.entities.File.id]
     */
    fun findAllByFile(file: File): List<LnkExecutionFile>

    /**
     * @param fileId id of [com.saveourtool.save.entities.File] that is connected to [com.saveourtool.save.entities.Execution]
     * @return [LnkExecutionFile] by [com.saveourtool.save.entities.File]
     */
    fun findAllByFileId(fileId: Long): List<LnkExecutionFile>
}
