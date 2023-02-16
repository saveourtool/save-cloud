package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.Storage
import com.saveourtool.save.storage.impl.AbstractInternalFileStorage
import com.saveourtool.save.storage.impl.InternalFileKey
import com.saveourtool.save.storage.StorageKotlinExtension.upload
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.github.GitHubHelper
import com.saveourtool.save.utils.github.GitHubRepo
import com.saveourtool.save.utils.warn
import generated.SAVE_CORE_VERSION
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Storage for internal files used by backend: save-cli and save-agent
 */
@Component
class BackendInternalFileStorage(
    configProperties: ConfigProperties,
    s3Operations: S3Operations,
) : AbstractInternalFileStorage(
    listOf(saveAgentKey),
    configProperties.s3Storage.prefix,
    s3Operations,
) {
    override fun doInitAdditionally(underlying: Storage<InternalFileKey>): Mono<Unit> = mono {
        GitHubHelper.availableTags(saveCliRepo)
            .sorted()
            .takeLast(3)
            .forEach { tagName ->
                val key = saveCliKey(tagName.removePrefix("v"))
                val isUploaded = GitHubHelper.download(saveCliRepo, tagName, key.name) { (content, contentLength) ->
                    underlying.upload(key, contentLength, content)
                }
                isUploaded?.run {
                    log.debug {
                        "Uploaded $key to internal storage"
                    }
                } ?: log.warn {
                    "Not found $key in github"
                }
            }
    }

    companion object {
        private val log: Logger = getLogger<BackendInternalFileStorage>()

        /**
         * [InternalFileKey] for *save-agent*
         */
        val saveAgentKey: InternalFileKey = InternalFileKey.latest("save-agent.kexe")

        /**
         * @param version
         * @return [InternalFileKey] for *save-cli* with version [version] ([SAVE_CORE_VERSION] is default)
         */
        fun saveCliKey(version: String = SAVE_CORE_VERSION): InternalFileKey = InternalFileKey(
            name = "save-$version-linuxX64.kexe",
            version = version,
        )

        val saveCliRepo = GitHubRepo(
            "saveourtool",
            "save-cli",
        )
    }
}
