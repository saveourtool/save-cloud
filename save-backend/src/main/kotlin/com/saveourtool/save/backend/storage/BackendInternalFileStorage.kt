package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.DefaultStorageProjectReactor
import com.saveourtool.save.storage.impl.AbstractInternalFileStorage
import com.saveourtool.save.storage.impl.InternalFileKey
import com.saveourtool.save.utils.*
import com.saveourtool.save.utils.github.GitHubHelper
import com.saveourtool.save.utils.github.GitHubRepo

import org.slf4j.Logger
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.flux
import kotlinx.coroutines.reactor.mono

/**
 * Storage for internal files used by backend: save-cli and save-agent
 */
@Component
class BackendInternalFileStorage(
    configProperties: ConfigProperties,
    private val s3Operations: S3Operations,
) : AbstractInternalFileStorage(
    listOf(InternalFileKey.saveAgentKey),
    configProperties.s3Storage.prefix,
    s3Operations,
) {
    override fun doInitAdditionally(underlying: DefaultStorageProjectReactor<InternalFileKey>): Mono<Unit> = mono(s3Operations.coroutineDispatcher) {
        GitHubHelper.availableTags(saveCliRepo)
            .sorted()
            .takeLast(SAVE_CLI_VERSIONS)
            .map { tagName ->
                InternalFileKey.saveCliKey(tagName.removePrefix("v")) to tagName
            }
    }
        .flatMapIterable { it }
        .filterWhen { (key, _) ->
            underlying.doesExist(key).map(Boolean::not)
        }
        .flatMap { (key, tagName) ->
            mono(s3Operations.coroutineDispatcher) {
                GitHubHelper.download(saveCliRepo, tagName, key.name) { (content, contentLength) ->
                    log.info {
                        "Uploaded $key to internal storage"
                    }
                    underlying.upload(key, contentLength, flux(s3Operations.coroutineDispatcher) { content.toByteBufferFlow() }).awaitSingle()
                } ?: log.warn {
                    "Not found $key in github"
                }
            }
        }
        .thenJust(Unit)

    companion object {
        private val log: Logger = getLogger<BackendInternalFileStorage>()
        private const val SAVE_CLI_VERSIONS = 3
        val saveCliRepo = GitHubRepo(
            "saveourtool",
            "save-cli",
        )
    }
}
