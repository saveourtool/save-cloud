package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.s3.S3OperationsProjectReactor
import com.saveourtool.save.storage.AbstractS3Storage
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.storage.s3KeyToPartsTill
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.utils.orNotFound
import org.springframework.stereotype.Service

/**
 * Storage for Avatars
 * Currently, key is (AvatarType, ObjectName)
 */
@Service
class AvatarStorage(
    configProperties: ConfigProperties,
    s3Operations: S3OperationsProjectReactor,
) : AbstractS3Storage<AvatarKey>(
    s3Operations,
    concatS3Key(configProperties.s3Storage.prefix, "images", "avatars")
) {
    override fun buildKey(s3KeySuffix: String): AvatarKey {
        val (typeStr, objectName) = s3KeySuffix.s3KeyToPartsTill(prefix)
        return AvatarKey(
            type = AvatarType.findByUrlPath(typeStr)
                .orNotFound {
                    "Not supported type for path: $typeStr"
                },
            objectName = objectName,
        )
    }

    override fun buildS3KeySuffix(key: AvatarKey): String = concatS3Key(key.type.urlPath, key.objectName)
}
