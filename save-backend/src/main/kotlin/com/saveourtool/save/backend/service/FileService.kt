package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.FileRepository
import com.saveourtool.save.entities.File
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.entities.Project
import com.saveourtool.save.utils.orNotFound
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class FileService(
    private val fileRepository: FileRepository,
) {
    fun get(id: Long): File = fileRepository.findByIdOrNull(id)
        .orNotFound { "Not found a file by id $id" }

    fun getByProjectAndName(
        project: Project,
        name: String,
        uploadedTime: LocalDateTime,
    ): File = fileRepository.findByProjectAndNameAndUploadedTime(
        project, name, uploadedTime
    )
        .orNotFound {
            "Not found a file with name $name and uploadedTime $uploadedTime in $project"
        }

    fun getByProject(
        project: Project,
    ): List<FileDto> = fileRepository.findAllByProject(project).map { it.toDto() }

    fun createNew(
        project: Project,
        name: String,
    ): File = fileRepository.save(
        File(
            project = project,
            name = name,
            uploadedTime = LocalDateTime.now(),
            sizeBytes = -1L,
            isExecutable = false,
        )
    )

    fun update(
        file: File,
        contentSizeInBytes: Long,
    ): File = fileRepository.save(
        file.apply {
            // to check that entry is saved already
            id = requiredId()
            sizeBytes = contentSizeInBytes
        }
    )
}