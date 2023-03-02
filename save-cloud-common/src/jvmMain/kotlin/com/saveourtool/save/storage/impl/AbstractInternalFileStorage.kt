package com.saveourtool.save.storage.impl

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.*
import com.saveourtool.save.storage.key.AbstractS3KeyManager
import com.saveourtool.save.storage.key.S3KeyManager
import com.saveourtool.save.utils.*

import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.extra.math.max
import java.net.URL
import javax.annotation.PostConstruct

/**
 * Storage for internal files used by backend and demo: save-cli and save-agent/save-demo-agent
 *
 * @param keysToLoadFromClasspath a list of keys which need to load on init
 * @param s3StoragePrefix a common prefix for s3 storage
 * @param s3Operations
 */
open class AbstractInternalFileStorage(
    private val keysToLoadFromClasspath: Collection<InternalFileKey>,
    s3StoragePrefix: String,
    s3Operations: S3Operations,
) {
    private val log: Logger = getLogger(this::class)
    private val initializer: StorageInitializer = StorageInitializer(this::class)
    private val s3KeyManager: S3KeyManager<InternalFileKey> = object : AbstractS3KeyManager<InternalFileKey>(concatS3Key(s3StoragePrefix, "internal-storage")) {
        override fun buildKeyFromSuffix(s3KeySuffix: String): InternalFileKey {
            val (name, version, fileName) = s3KeySuffix.s3KeyToParts()
            return InternalFileKey(
                name = name,
                version = version,
                fileName = fileName,
            )
        }
        override fun buildS3KeySuffix(key: InternalFileKey): String = concatS3Key(key.name, key.version, key.fileName)
    }
    private val storageProjectReactor by lazy { DefaultStorageProjectReactor(s3Operations, s3KeyManager) }
    private val storageCoroutines by lazy { DefaultStorageCoroutines(s3Operations, s3KeyManager) }
    private val storagePreSignedUrl by lazy { DefaultStoragePreSignedUrl(s3Operations, s3KeyManager) }

    /**
     * An init method to upload internal files to S3 from classpath or github
     */
    @PostConstruct
    fun doInit() {
        initializer.init(
            doInitReactively = {
                Flux.concat(
                    keysToLoadFromClasspath.toFlux()
                        .flatMap {
                            storageProjectReactor.uploadFromClasspath(it)
                        },
                    doInitAdditionally(storageProjectReactor)
                ).last()
            },
            doInitSuspendedly = { doInitAdditionally(storageCoroutines) }
        )
    }

    /**
     * Async method to init this storage: copy required files to storage
     *
     * @param underlying
     * @return [Mono] without body
     */
    protected open fun doInitAdditionally(underlying: DefaultStorageProjectReactor<InternalFileKey>): Mono<Unit> = Mono.empty()

    /**
     * Suspend method to init this storage: copy required files to storage
     *
     * @param underlying
     * @return [Mono] without body
     */
    protected open suspend fun doInitAdditionally(underlying: DefaultStorageCoroutines<InternalFileKey>): Unit? = null

    /**
     * @param key
     * @return generated [URL] to download provided [key] [InternalFileKey]
     * @throws ResponseStatusException with status [HttpStatus.NOT_FOUND]
     */
    fun generateRequiredUrlToDownload(key: InternalFileKey): URL = storagePreSignedUrl.generateUrlToDownload(key)
        .orNotFound {
            "Not found $key in internal storage"
        }

    /**
     * @param name [InternalFileKey.name]
     * @return [Mono] with newer or latest [InternalFileKey] with provided [name] in internal storage or [Mono.error] if it's not found.
     */
    fun generateUrlToDownloadNewerOrLatest(name: String): Mono<URL> {
        return storageProjectReactor
            .list()
                .filter { it.name == name }
                .max(InternalFileKey.versionCompartor)
                .switchIfEmptyToNotFound {
                    "Not found newer $name in internal storage"
                }
            .map { generateRequiredUrlToDownload(it) }
    }

    /**
     * @param function
     * @return result of [function] which is run using [StorageProjectReactor]
     */
    fun <T> usingProjectReactor(function: StorageProjectReactor<InternalFileKey>.() -> T): T = initializer.validateAndRun {
        function(storageProjectReactor)
    }

    /**
     * @param function
     * @return result of [function] which is run using [StorageCoroutines]
     */
    suspend fun <T> usingCoroutines(function: suspend StorageCoroutines<InternalFileKey>.() -> T): T = initializer.validateAndRunSuspend {
        function(storageCoroutines)
    }

    /**
     * @param function
     * @return result of [function] which is run using [StoragePreSignedUrl]
     */
    fun <T> usingPreSignedUrl(function: StoragePreSignedUrl<InternalFileKey>.() -> T): T = initializer.validateAndRun {
        function(storagePreSignedUrl)
    }

    private fun StorageProjectReactor<InternalFileKey>.uploadFromClasspath(key: InternalFileKey): Mono<Unit> = doesExist(key)
        .filterWhen { exists ->
            if (exists && key.isLatest()) {
                delete(key)
            } else {
                exists.not().toMono()
            }
        }
        .flatMap {
            tryDownloadFromClasspath(key.fileName)
                .flatMap { resource ->
                    upload(
                        key,
                        resource.contentLength(),
                        resource.toByteBufferFlux(),
                    )
                }
                .effectIfEmpty {
                    log.info {
                        "There is no ${key.name} with version ${key.version} in classpath"
                    }
                }
        }
        .thenReturn(Unit)
        .defaultIfEmpty(Unit)
}
