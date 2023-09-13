package com.saveourtool.save.cosv.storage

import com.saveourtool.save.cosv.repository.CosvObjectRepository
import com.saveourtool.save.entities.cosv.CosvFile
import com.saveourtool.save.s3.S3OperationsProperties
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.storage.key.AbstractS3KeyEntityManager
import com.saveourtool.save.utils.BlockingBridge
import org.springframework.stereotype.Component

/**
 * S3KeeManager for [CosvFile]
 */
@Component
class CosvFileS3KeyManager(
    s3OperationsPropertiesProvider: S3OperationsProperties.Provider,
    cosvObjectRepository: CosvObjectRepository,
    blockingBridge: BlockingBridge,
) : AbstractS3KeyEntityManager<CosvFile, CosvObjectRepository>(
    prefix = concatS3Key(s3OperationsPropertiesProvider.s3Storage.prefix, "cosv"),
    repository = cosvObjectRepository,
    blockingBridge = blockingBridge,
) {
    override fun findByContent(key: CosvFile): CosvFile? = repository.findByIdentifierAndModified(key.identifier, key.modified)
}