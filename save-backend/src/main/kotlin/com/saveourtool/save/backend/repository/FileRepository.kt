package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.File
import com.saveourtool.save.entities.Project
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface FileRepository : BaseEntityRepository<File> {
    fun findByProjectAndNameAndUploadedTime(
        project: Project,
        name: String,
        uploadedTime: LocalDateTime,
    ): File?

    fun findAllByProject(
        project: Project,
    ): List<File>
}
