package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.File
import com.saveourtool.save.entities.Project
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [File]
 */
@Repository
interface FileRepository : BaseEntityRepository<File> {
    /**
     * @param project
     * @return all [File]s which [project] does contain
     */
    fun findAllByProject(
        project: Project,
    ): List<File>
}
