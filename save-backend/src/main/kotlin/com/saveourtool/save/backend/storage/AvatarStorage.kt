package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.*
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
    s3Operations: S3Operations,
) : AbstractSimpleStorage<AvatarKey>(
    s3Operations,
    concatS3Key(configProperties.s3Storage.prefix, "images", "avatars")
) {
    override fun doBuildKeyFromSuffix(s3KeySuffix: String): AvatarKey {
        val (typeStr, objectName) = s3KeySuffix.split(PATH_DELIMITER)
        return AvatarKey(
            type = AvatarType.findByUrlPath(typeStr)
                .orNotFound {
                    "Not supported type for path: $typeStr"
                },
            objectName = objectName,
        )
    }

    override fun doBuildS3KeySuffix(key: AvatarKey): String = concatS3Key(key.type.urlPath, key.objectName)
}
