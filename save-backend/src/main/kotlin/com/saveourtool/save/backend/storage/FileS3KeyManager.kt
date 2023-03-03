package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.FileRepository
import com.saveourtool.save.backend.service.ExecutionService
import com.saveourtool.save.backend.service.ProjectService
import com.saveourtool.save.entities.File
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.entities.Project
import com.saveourtool.save.entities.toEntity
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.storage.key.AbstractS3KeyDtoManager
import com.saveourtool.save.utils.BlockingBridge
import com.saveourtool.save.utils.orNotFound

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

import kotlinx.datetime.toJavaLocalDateTime

/**
 * [com.saveourtool.save.storage.key.S3KeyManager] for [FileStorage]
 */
@Component
class FileS3KeyManager(
    configProperties: ConfigProperties,
    fileRepository: FileRepository,
    blockingBridge: BlockingBridge,
    private val projectService: ProjectService,
    private val executionService: ExecutionService,
) : AbstractS3KeyDtoManager<FileDto, File, FileRepository>(
    concatS3Key(configProperties.s3Storage.prefix, "storage"),
    fileRepository,
    blockingBridge,
) {
    override fun createNewEntityFromDto(dto: FileDto): File = dto.toEntity {
        projectService.findByNameAndOrganizationNameAndCreatedStatus(dto.projectCoordinates.projectName, dto.projectCoordinates.organizationName)
            .orNotFound {
                "Not found project by coordinates: ${dto.projectCoordinates}"
            }
    }

    override fun findByDto(dto: FileDto): File? = repository.findByProject_Organization_NameAndProject_NameAndNameAndUploadedTime(
        organizationName = dto.projectCoordinates.organizationName,
        projectName = dto.projectCoordinates.projectName,
        name = dto.name,
        uploadedTime = dto.uploadedTime.toJavaLocalDateTime(),
    )

    override fun beforeDelete(entity: File) {
        executionService.unlinkFileFromAllExecution(entity)
    }

    /**
     * @param project
     * @return all [FileDto]s which provided [Project] does contain
     */
    fun listByProject(
        project: Project,
    ): Collection<FileDto> = repository.findAllByProject(project).map { it.toDto() }

    /**
     * @param fileId
     * @return [FileDto] for [File] with provided [fileId]
     */
    fun findFileById(
        fileId: Long,
    ): FileDto? = repository.findByIdOrNull(fileId)?.toDto()
}
