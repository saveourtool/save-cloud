package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.FileRepository
import com.saveourtool.save.entities.File
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.entities.Project
import com.saveourtool.save.utils.orNotFound
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Service for [FileRepository]
 */
@Service
class FileService(
    private val fileRepository: FileRepository,
) {
    /**
     * @param id
     * @return [File] with provided [id]
     */
    fun get(id: Long): File = fileRepository.findByIdOrNull(id)
        .orNotFound { "Not found a file by id $id" }

    /**
     * @param project
     * @return all [FileDto]s which provided [Project] does contain
     */
    fun getByProject(
        project: Project,
    ): List<FileDto> = fileRepository.findAllByProject(project).map { it.toDto() }

    /**
     * @param project
     * @param name
     * @return saved [File]
     */
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

    /**
     * @param file
     * @param contentSizeInBytes
     * @return updated [File]
     */
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
