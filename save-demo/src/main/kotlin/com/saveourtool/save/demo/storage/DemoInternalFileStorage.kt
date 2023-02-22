package com.saveourtool.save.demo.storage

import com.saveourtool.save.demo.config.ConfigProperties
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.impl.AbstractInternalFileStorageUsingProjectReactor
import com.saveourtool.save.storage.impl.InternalFileKey
import org.springframework.stereotype.Component

/**
 * Storage for internal files used by demo: save-demo-agent
 */
@Component
class DemoInternalFileStorage(
    configProperties: ConfigProperties,
    s3Operations: S3Operations,
) : AbstractInternalFileStorageUsingProjectReactor(
    listOf(saveDemoAgent),
    configProperties.s3Storage.prefix,
    s3Operations,
) {
    companion object {
        /**
         * [InternalFileKey] for *save-demo-agent*
         */
        val saveDemoAgent: InternalFileKey = InternalFileKey.latest("save-demo-agent.kexe")
    }
}
