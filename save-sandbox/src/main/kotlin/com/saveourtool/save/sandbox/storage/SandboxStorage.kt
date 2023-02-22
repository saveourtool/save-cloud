package com.saveourtool.save.sandbox.storage

import com.saveourtool.save.s3.S3OperationsProjectReactor
import com.saveourtool.save.sandbox.config.ConfigProperties
import com.saveourtool.save.storage.AbstractSimpleStorageUsingProjectReactor
import com.saveourtool.save.storage.PATH_DELIMITER
import com.saveourtool.save.storage.concatS3Key
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

/**
 * Storage implementation for sandbox
 */
@Component
class SandboxStorage(
    configProperties: ConfigProperties,
    s3Operations: S3OperationsProjectReactor,
) : AbstractSimpleStorageUsingProjectReactor<SandboxStorageKey>(
    s3Operations,
    concatS3Key(configProperties.s3Storage.prefix, "sandbox"),
) {
    @Suppress("DestructuringDeclarationWithTooManyEntries")
    override fun doBuildKeyFromSuffix(s3KeySuffix: String): SandboxStorageKey {
        val (userId, typeName, filename) = s3KeySuffix.split(PATH_DELIMITER)
        return SandboxStorageKey(
            userId.toLong(),
            SandboxStorageKeyType.valueOf(typeName),
            filename,
        )
    }

    override fun doBuildS3KeySuffix(key: SandboxStorageKey): String =
            concatS3Key(key.userId.toString(), key.type.name, key.fileName)

    /**
     * @param userId
     * @param types
     * @return list of keys in storage with requested [SandboxStorageKey.type] and [SandboxStorageKey.userId]
     */
    fun list(
        userId: Long,
        vararg types: SandboxStorageKeyType
    ): Flux<SandboxStorageKey> = list().filter {
        it.userId == userId && it.type in types
    }
}
