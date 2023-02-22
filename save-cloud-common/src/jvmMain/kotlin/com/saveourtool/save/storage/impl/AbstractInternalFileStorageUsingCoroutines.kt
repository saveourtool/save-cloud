package com.saveourtool.save.storage.impl

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.*
import com.saveourtool.save.utils.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono

/**
 * Storage for internal files used by backend and demo: save-cli and save-agent/save-demo-agent
 *
 * @param keysToLoadFromClasspath a list of keys which need to load on init
 * @param s3StoragePrefix a common prefix for s3 storage
 * @param s3Operations
 */
abstract class AbstractInternalFileStorageUsingCoroutines(
    private val keysToLoadFromClasspath: Collection<InternalFileKey>,
    s3StoragePrefix: String,
    s3Operations: S3Operations,
) : AbstractSimpleStorageUsingCoroutines<InternalFileKey>(
    s3Operations,
    concatS3Key(s3StoragePrefix, "internal-storage")
) {
    /**
     * An init method to upload internal files to S3 from classpath or github
     *
     * @param underlying
     * @return [Mono] without body
     */
    override fun doInit(underlying: DefaultStorageProjectReactor<InternalFileKey>): Mono<Unit> = Flux.concat(
        keysToLoadFromClasspath.toFlux()
            .flatMap {
                underlying.uploadFromClasspath(it, it.isLatest())
            },
        doInitAdditionally(underlying),
    )
        .last()

    /**
     * Async method to init this storage: copy required files to storage
     *
     * @param underlying
     * @return [Mono] without body
     */
    protected open fun doInitAdditionally(underlying: Storage<InternalFileKey>): Mono<Unit> = Mono.just(Unit)

    override fun doBuildKeyFromSuffix(s3KeySuffix: String): InternalFileKey {
        val (version, name) = s3KeySuffix.s3KeyToParts()
        return InternalFileKey(
            name = name,
            version = version,
        )
    }

    override fun doBuildS3KeySuffix(key: InternalFileKey): String = concatS3Key(key.version, key.name)
}
