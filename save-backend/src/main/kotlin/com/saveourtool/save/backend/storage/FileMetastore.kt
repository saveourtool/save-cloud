package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.FileRepository
import com.saveourtool.save.backend.service.ExecutionService
import com.saveourtool.save.backend.service.ProjectService
import com.saveourtool.save.entities.File
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.entities.toEntity
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.storage.key.DatabaseDtoMetastore
import com.saveourtool.save.utils.orNotFound
import kotlinx.datetime.toJavaLocalDateTime
import org.springframework.stereotype.Component

@Component
class FileMetastore(
    configProperties: ConfigProperties,
    repository: FileRepository,
    private val projectService: ProjectService,
    private val executionService: ExecutionService,
) : DatabaseDtoMetastore<FileDto, File, FileRepository>(
    concatS3Key(configProperties.s3Storage.prefix, "storage"),
    repository,
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
}