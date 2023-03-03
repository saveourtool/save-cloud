package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.DefaultStorageCoroutines
import com.saveourtool.save.storage.impl.AbstractInternalFileStorage
import com.saveourtool.save.storage.impl.InternalFileKey
import com.saveourtool.save.utils.*
import com.saveourtool.save.utils.github.GitHubHelper
import com.saveourtool.save.utils.github.GitHubRepoInfo
import generated.SAVE_CORE_VERSION

import org.slf4j.Logger
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
        GitHubHelper.availableTags(saveCliRepo)
            .sorted()
            .takeLast(SAVE_CLI_VERSIONS)
            .map { tagName ->
                InternalFileKey.saveCliKey(tagName.removePrefix("v")) to tagName
            }
            .filterNot { (key, _) ->
                underlying.doesExist(key)
            }
            .forEach { (key, tagName) ->
                GitHubHelper.download(saveCliRepo, tagName, key.fileName) { (content, contentLength) ->
                    val uploadedKey = underlying.upload(key, contentLength, content.toByteBufferFlow())
                    log.info {
                        "Uploaded $uploadedKey to internal storage"
                    }
                } ?: log.warn {
                    "Not found $key in github"
                }
            }
    }

    companion object {
        private val log: Logger = getLogger<BackendInternalFileStorage>()
        private const val SAVE_CLI_VERSIONS = 3
        private val saveCliRepo = object : GitHubRepoInfo {
            override val organizationName: String = "saveourtool"
            override val projectName: String = "save-cli"
        }
    }
}
