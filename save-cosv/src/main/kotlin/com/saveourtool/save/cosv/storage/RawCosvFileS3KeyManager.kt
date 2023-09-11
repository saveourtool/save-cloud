package com.saveourtool.save.cosv.storage

import com.saveourtool.save.backend.service.IBackendService
import com.saveourtool.save.cosv.repository.RawCosvFileRepository
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.RawCosvFile
import com.saveourtool.save.entities.cosv.RawCosvFile.Companion.toNewEntity
import com.saveourtool.save.entities.cosv.RawCosvFileDto
import com.saveourtool.save.entities.cosv.RawCosvFileStatus
import com.saveourtool.save.s3.S3OperationsProperties
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.storage.key.AbstractS3KeyDtoManager
import com.saveourtool.save.utils.BlockingBridge
import com.saveourtool.save.utils.getByIdOrNotFound
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * S3KeyManager for [RawCosvFileStorage]
 */
@Component
class RawCosvFileS3KeyManager(
    s3OperationsPropertiesProvider: S3OperationsProperties.Provider,
    rawCosvFileRepository: RawCosvFileRepository,
    private val backendService: IBackendService,
    blockingBridge: BlockingBridge,
) : AbstractS3KeyDtoManager<RawCosvFileDto, RawCosvFile, RawCosvFileRepository>(
    prefix = concatS3Key(s3OperationsPropertiesProvider.s3Storage.prefix, "raw-cosv"),
    repository = rawCosvFileRepository,
    blockingBridge = blockingBridge,
) {
    override fun findByDto(dto: RawCosvFileDto): RawCosvFile? = repository.findByOrganizationNameAndUserNameAndFileName(
        organizationName = dto.organizationName,
        userName = dto.userName,
        fileName = dto.fileName,
    )

    override fun createNewEntityFromDto(dto: RawCosvFileDto): RawCosvFile =
            dto.toNewEntity(backendService::getUserByName, backendService::getOrganizationByName)

    /**
     * @param organizationName
     * @return all [RawCosvFileDto]s which has provided [RawCosvFileDto.organizationName]
     */
    fun listByOrganization(
        organizationName: String,
    ): Collection<RawCosvFileDto> = repository.findAllByOrganizationName(organizationName).map { it.toDto() }

    /**
     * @param ids
     * @param newStatus
     */
    @Transactional
    fun markAs(
        ids: Collection<Long>,
        newStatus: RawCosvFileStatus,
    ) {
        repository.saveAll(repository.findAllById(ids).map { entry -> entry.apply { status = newStatus } })
    }

    /**
     * @param id
     * @return [Organization] to which is uploaded and [User] who uploaded
     */
    fun getOrganizationAndOwner(
        id: Long,
    ): Pair<Organization, User> = repository.getByIdOrNotFound(id).let {
        it.organization to it.user
    }
}
