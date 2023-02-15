package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.impl.AbstractInternalFileStorage
import com.saveourtool.save.storage.impl.InternalFileKey
import com.saveourtool.save.storage.upload
import com.saveourtool.save.utils.deferredToMono
import com.saveourtool.save.utils.github.GitHubHelper
import com.saveourtool.save.utils.github.GitHubRepo
import generated.SAVE_CORE_VERSION
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun doInitAdditionally(): Mono<Unit> = deferredToMono {
        scope.async {
            GitHubHelper.availableTags(saveCliRepo)
                .forEach { tagName ->
                    val key = saveCliKey(tagName)
                    val (content, contentLength) = GitHubHelper.download(saveCliRepo, tagName, key.name)
                    upload(key, contentLength, content)
                }
        }
    }

    companion object {
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
