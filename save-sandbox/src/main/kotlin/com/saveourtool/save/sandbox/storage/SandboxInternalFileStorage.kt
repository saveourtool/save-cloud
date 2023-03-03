package com.saveourtool.save.sandbox.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.sandbox.config.ConfigProperties
import com.saveourtool.save.storage.DefaultStorageCoroutines
import com.saveourtool.save.storage.impl.AbstractInternalFileStorage
import com.saveourtool.save.storage.impl.InternalFileKey
import generated.SAVE_CORE_VERSION
import org.springframework.stereotype.Component

/**
 * Storage for internal files used by sandbox: save-cli and save-agent
 */
@Component
class SandboxInternalFileStorage(
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
