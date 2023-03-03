package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.DefaultStorageCoroutines
import com.saveourtool.save.storage.impl.AbstractInternalFileStorage
import com.saveourtool.save.storage.impl.InternalFileKey
import com.saveourtool.save.utils.*
import generated.SAVE_CORE_VERSION

import org.springframework.stereotype.Component

/**
 * Storage for internal files used by backend: save-cli and save-agent
 */
@Component
class BackendInternalFileStorage(
    configProperties: ConfigProperties,
    s3Operations: S3Operations,
) : AbstractInternalFileStorage(
    listOf(InternalFileKey.saveAgentKey, InternalFileKey.saveCliKey(SAVE_CORE_VERSION)),
    configProperties.s3Storage.prefix,
    s3Operations,
) {
    override suspend fun doInitAdditionally(underlying: DefaultStorageCoroutines<InternalFileKey>) {
        underlying.downloadSaveCliFromGithub()
    }
}
