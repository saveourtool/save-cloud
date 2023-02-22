package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.Storage
import com.saveourtool.save.storage.impl.AbstractInternalFileStorageUsingProjectReactor
import com.saveourtool.save.storage.impl.InternalFileKey
import com.saveourtool.save.utils.github.GitHubHelper
import com.saveourtool.save.utils.github.GitHubRepo
import com.saveourtool.save.utils.info
import com.saveourtool.save.utils.thenJust
import com.saveourtool.save.utils.toByteBufferFlow
import com.saveourtool.save.utils.warn

import generated.SAVE_CORE_VERSION
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

import kotlinx.coroutines.reactor.flux
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.withContext

/**
 * Storage for internal files used by backend: save-cli and save-agent
 */
@Component
class BackendInternalFileStorage(
    configProperties: ConfigProperties,
    s3Operations: S3Operations,
) : AbstractInternalFileStorageUsingProjectReactor(
    listOf(saveAgentKey),
    configProperties.s3Storage.prefix,
    s3Operations,
) {
    override fun doInitAdditionally(underlying: Storage<InternalFileKey>): Mono<Unit> = flux(initCoroutineDispatcher) {
        GitHubHelper.availableTags(saveCliRepo)
            .sorted()
            .takeLast(SAVE_CLI_VERSIONS)
            .map { tagName ->
                saveCliKey(tagName.removePrefix("v")) to tagName
            }
            .forEach { this.send(it) }
    }
        .filterWhen { (key, _) ->
            underlying.doesExist(key).map(Boolean::not)
        }
        .flatMap { (key, tagName) ->
            mono(initCoroutineDispatcher) {
                withContext(initCoroutineDispatcher) {
                    GitHubHelper.download(saveCliRepo, tagName, key.name) { (content, contentLength) ->
                        underlying.upload(key, contentLength, content.toByteBufferFlow())
                        log.info {
                            "Uploaded $key to internal storage"
                        }
                    } ?: log.warn {
                        "Not found $key in github"
                    }
                }
            }
        }
        .thenJust(Unit)

    companion object {
        private const val SAVE_CLI_VERSIONS = 3

        /**
         * [InternalFileKey] for *save-agent*
         */
        val saveAgentKey: InternalFileKey = InternalFileKey.latest("save-agent.kexe")
        val saveCliRepo = GitHubRepo(
            "saveourtool",
            "save-cli",
        )

        /**
         * @param version
         * @return [InternalFileKey] for *save-cli* with version [version] ([SAVE_CORE_VERSION] is default)
         */
        fun saveCliKey(version: String = SAVE_CORE_VERSION): InternalFileKey = InternalFileKey(
            name = "save-$version-linuxX64.kexe",
            version = version,
        )
    }
}
