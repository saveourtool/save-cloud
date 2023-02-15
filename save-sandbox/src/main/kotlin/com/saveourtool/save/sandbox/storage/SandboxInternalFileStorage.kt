package com.saveourtool.save.sandbox.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.sandbox.config.ConfigProperties
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
    listOf(saveAgentKey, saveCliKey),
    configProperties.s3Storage.prefix,
    s3Operations,
) {
    companion object {
        /**
         * [InternalFileKey] for *save-agent*
         */
        val saveAgentKey: InternalFileKey = InternalFileKey.latest("save-agent.kexe")

        /**
         * [InternalFileKey] for *save-cli* with version [SAVE_CORE_VERSION]
         */
        val saveCliKey: InternalFileKey = InternalFileKey(
            name = "save-$SAVE_CORE_VERSION-linuxX64.kexe",
            version = SAVE_CORE_VERSION,
        )
    }
}