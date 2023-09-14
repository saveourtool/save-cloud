package com.saveourtool.save.cosv.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.s3.S3OperationsProperties
import com.saveourtool.save.storage.AbstractSimpleReactiveStorage
import com.saveourtool.save.storage.PATH_DELIMITER
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.storage.s3KeyToParts
import com.saveourtool.save.utils.getCurrentLocalDateTime

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

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
    override fun list(): Flux<CosvKey> {
        // a workaround to support migration
        return super.list().filter { it.isValid }
    }

    override fun doBuildKeyFromSuffix(s3KeySuffix: String): CosvKey = if (s3KeySuffix.removeSuffix(PATH_DELIMITER).removePrefix(PATH_DELIMITER).contains(PATH_DELIMITER)) {
        val (id, modified) = s3KeySuffix.s3KeyToParts()
        CosvKey(id, LocalDateTime.parse(modified.replace('_', ':')))
    } else {
        CosvKey(s3KeySuffix, getCurrentLocalDateTime(), false)
    }

    override fun doBuildS3KeySuffix(key: CosvKey): String = concatS3Key(key.id, key.modified.toString().replace(':', '_'))
}
