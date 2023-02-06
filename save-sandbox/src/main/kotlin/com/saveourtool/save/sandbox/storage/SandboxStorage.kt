package com.saveourtool.save.sandbox.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.sandbox.config.ConfigProperties
import com.saveourtool.save.storage.AbstractS3Storage
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.storage.s3KeyToPartsTill
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

/**
 * Storage implementation for sandbox
 */
@Component
class SandboxStorage(
    configProperties: ConfigProperties,
    s3Operations: S3Operations,
) : AbstractS3Storage<SandboxStorageKey>(
    s3Operations,
    concatS3Key(configProperties.s3Storage.prefix, "sandbox"),
) {
    @Suppress("DestructuringDeclarationWithTooManyEntries")
    override fun buildKey(s3KeySuffix: String): SandboxStorageKey {
        val (filename, typeName, userId) = s3KeySuffix.s3KeyToPartsTill(prefix)
        return SandboxStorageKey(
            userId.toLong(),
            SandboxStorageKeyType.valueOf(typeName),
            filename,
        )
    }

    override fun buildS3KeySuffix(key: SandboxStorageKey): String =
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
