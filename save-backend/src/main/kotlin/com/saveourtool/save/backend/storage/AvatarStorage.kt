package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.AbstractS3Storage
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.storage.s3KeyToParts
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.utils.orNotFound
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer

/**
 * Storage for Avatars
 * Currently, key is (AvatarType, ObjectName)
 */
@Service
class AvatarStorage(
    configProperties: ConfigProperties,
    s3Operations: S3Operations,
) : AbstractS3Storage<AvatarKey>(
    s3Operations,
    concatS3Key(configProperties.s3Storage.prefix, "images", "avatars")
) {
    override fun buildKey(s3KeySuffix: String): AvatarKey {
        val (typeStr, objectName) = s3KeySuffix.s3KeyToParts()
        return AvatarKey(
            type = AvatarType.findByUrlPath(typeStr)
                .orNotFound {
                    "Not supported type for path: $typeStr"
                },
            objectName = objectName,
        )
    }

    override fun buildS3KeySuffix(key: AvatarKey): String = concatS3Key(key.type.urlPath, key.objectName)

    /**
     * @param key
     * @param content
     * @param contentLength
     * @return `Mono` with file size
     */
    fun upsert(key: AvatarKey, contentLength: Long, content: Flux<ByteBuffer>): Mono<Long> = list()
        .filter { it == key }
        .singleOrEmpty()
        .flatMap { delete(it) }
        .switchIfEmpty(Mono.just(true))
        .flatMap { upload(key, contentLength, content) }
        .thenReturn(contentLength)
}
