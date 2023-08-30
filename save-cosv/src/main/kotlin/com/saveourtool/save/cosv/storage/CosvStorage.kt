package com.saveourtool.save.cosv.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.s3.S3OperationsProperties
import com.saveourtool.save.storage.AbstractSimpleReactiveStorage
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.storage.s3KeyToParts

import org.springframework.stereotype.Service

import kotlinx.datetime.LocalDateTime

/**
 * Storage for Vulnerabilities.
 *
 * For now, we store vulnerabilities in S3 with `id` as a key.
 * We can migrate to NoSql database in the future.
 */
@Service
class CosvStorage(
    s3OperationsPropertiesProvider: S3OperationsProperties.Provider,
    s3Operations: S3Operations,
) : AbstractSimpleReactiveStorage<CosvKey>(
    s3Operations,
    concatS3Key(s3OperationsPropertiesProvider.s3Storage.prefix, "cosv"),
) {
    override fun doBuildKeyFromSuffix(s3KeySuffix: String): CosvKey {
        val (id, modified) = s3KeySuffix.s3KeyToParts()
        return CosvKey(id, LocalDateTime.parse(modified))
    }

    override fun doBuildS3KeySuffix(key: CosvKey): String = concatS3Key(key.id, key.modified.toString())
}
