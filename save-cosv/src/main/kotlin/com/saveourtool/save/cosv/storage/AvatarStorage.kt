package com.saveourtool.save.cosv.storage

import com.saveourtool.save.cosv.configs.ConfigProperties
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.AbstractSimpleReactiveStorage
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.storage.s3KeyToParts
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
) : AbstractSimpleReactiveStorage<AvatarKey>(
    s3Operations,
    concatS3Key(configProperties.s3Storage.prefix, "images", "avatars")
) {
    override fun doBuildKeyFromSuffix(s3KeySuffix: String): AvatarKey {
        val (typeStr, objectName) = s3KeySuffix.s3KeyToParts()
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
