package com.saveourtool.save.cosv.storage

import com.saveourtool.save.backend.service.IBackendService
import com.saveourtool.save.cosv.repository.RawCosvFileRepository
import com.saveourtool.save.entities.cosv.RawCosvFile
import com.saveourtool.save.entities.cosv.RawCosvFile.Companion.toNewEntity
import com.saveourtool.save.entities.cosv.RawCosvFileDto
import com.saveourtool.save.s3.S3OperationsProperties
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.storage.key.AbstractS3KeyDtoManager
import com.saveourtool.save.utils.BlockingBridge
import org.springframework.stereotype.Component

/**
 * S3KeyManager for [RawCosvFileStorage]
 */
@Component
class RawCosvFileS3KeyManager(
    s3OperationsPropertiesProvider: S3OperationsProperties.Provider,
    private val rawCosvFileRepository: RawCosvFileRepository,
    private val backendService: IBackendService,
    blockingBridge: BlockingBridge,
) : AbstractS3KeyDtoManager<RawCosvFileDto, RawCosvFile, RawCosvFileRepository>(
    prefix = concatS3Key(s3OperationsPropertiesProvider.s3Storage.prefix, "raw-cosv"),
    repository = rawCosvFileRepository,
    blockingBridge = blockingBridge,
) {
    override fun findByDto(dto: RawCosvFileDto): RawCosvFile? = rawCosvFileRepository.findByUserNameAndFileName(dto.userName, dto.fileName)

    override fun createNewEntityFromDto(dto: RawCosvFileDto): RawCosvFile =
            dto.toNewEntity(backendService::getUserByName, backendService::getOrganizationByName)
}