package com.saveourtool.save.cosv.storage

import com.saveourtool.save.cosv.repository.CosvFileRepository
import com.saveourtool.save.entities.cosv.CosvFile
import com.saveourtool.save.entities.cosv.VulnerabilityMetadata
import com.saveourtool.save.s3.S3OperationsProperties
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.storage.key.AbstractS3KeyEntityManager
import com.saveourtool.save.utils.BlockingBridge
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * S3KeeManager for [CosvFile]
 */
@Component
class CosvFileS3KeyManager(
    s3OperationsPropertiesProvider: S3OperationsProperties.Provider,
    cosvFileRepository: CosvFileRepository,
    blockingBridge: BlockingBridge,
) : AbstractS3KeyEntityManager<CosvFile, CosvFileRepository>(
    prefix = concatS3Key(s3OperationsPropertiesProvider.s3Storage.prefix, "cosv"),
    repository = cosvFileRepository,
    blockingBridge = blockingBridge,
) {
    override fun findByContent(key: CosvFile): CosvFile? = repository.findByIdentifierAndModified(key.identifier, key.modified)

    /**
     * @param identifier
     * @return latest [CosvFile] by [identifier]
     */
    fun findLatest(identifier: String): CosvFile? = repository.findByIdentifierAndTopByModified(identifier)

    /**
     * Updates [cosvFile] by setting a link to prev cosv file and to [vulnerabilityMetadata]
     *
     * @param cosvFile
     * @param vulnerabilityMetadata
     * @return updated [cosvFile]
     */
    @Transactional
    fun updateByVulnerabilityMetadata(
        cosvFile: CosvFile,
        vulnerabilityMetadata: VulnerabilityMetadata,
    ): CosvFile = repository.save(
        cosvFile.apply {
            this.prevCosvFile = vulnerabilityMetadata.requiredLatestCosvFile()
            this.vulnerabilityMetadata = vulnerabilityMetadata
        }
    )
}
