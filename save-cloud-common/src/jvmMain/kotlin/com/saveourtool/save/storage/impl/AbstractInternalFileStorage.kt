package com.saveourtool.save.storage.impl

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.*
import com.saveourtool.save.storage.key.AbstractS3KeyManager
import com.saveourtool.save.storage.key.S3KeyManager
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
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
    private val initializer: StorageInitializer = StorageInitializer(this::class)
    private val s3KeyManager: S3KeyManager<InternalFileKey> = object : AbstractS3KeyManager<InternalFileKey>(concatS3Key(s3StoragePrefix, "internal-storage")) {
        override fun buildKeyFromSuffix(s3KeySuffix: String): InternalFileKey {
            val (version, name) = s3KeySuffix.s3KeyToParts()
            return InternalFileKey(
                name = name,
                version = version,
            )
        }
        override fun buildS3KeySuffix(key: InternalFileKey): String = concatS3Key(key.version, key.name)
    }
    private val storageProjectReactor by lazy { DefaultStorageProjectReactor(s3Operations, s3KeyManager) }
    private val storagePreSignedUrl by lazy { DefaultStoragePreSignedUrl(s3Operations, s3KeyManager) }

    /**
     * An init method to upload internal files to S3 from classpath or github
     */
    @PostConstruct
    fun doInit() {
        initializer.init {
            Flux.concat(
                keysToLoadFromClasspath.toFlux()
                    .flatMap {
                        storageProjectReactor.uploadFromClasspath(it)
                    },
                doInitAdditionally(storageProjectReactor)
            ).last()
        }
    }

    /**
     * Async method to init this storage: copy required files to storage
     *
     * @param underlying
     * @return [Mono] without body
     */
    protected open fun doInitAdditionally(underlying: DefaultStorageProjectReactor<InternalFileKey>): Mono<Unit> = Mono.empty()

    /**
     * @param function
     * @return result of [function] which is run using [StorageProjectReactor]
     */
    fun <T> usingProjectReactor(function: StorageProjectReactor<InternalFileKey>.() -> T): T = initializer.validateAndRun {
        function(storageProjectReactor)
    }

    /**
     * @param function
     * @return result of [function] which is run using [StoragePreSignedUrl]
     */
    fun <T> usingPreSignedUrl(function: StoragePreSignedUrl<InternalFileKey>.() -> T): T = initializer.validateAndRun {
        function(storagePreSignedUrl)
    }
}
