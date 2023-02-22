package com.saveourtool.save.storage.impl

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.*
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

/**
 * Storage for internal files used by backend and demo: save-cli and save-agent/save-demo-agent
 *
 * @param keysToLoadFromClasspath a list of keys which need to load on init
 * @param s3StoragePrefix a common prefix for s3 storage
 * @param s3Operations
 */
abstract class AbstractInternalFileStorageUsingProjectReactor(
    private val keysToLoadFromClasspath: Collection<InternalFileKey>,
    s3StoragePrefix: String,
    s3Operations: S3Operations,
) : AbstractSimpleStorageUsingProjectReactor<InternalFileKey>(
    s3Operations,
    concatS3Key(s3StoragePrefix, "internal-storage")
) {
    /**
     * An init method to upload internal files to S3 from classpath or github
     *
     * @param underlying
     * @return [Mono] without body
     */
    override fun doInit(underlying: DefaultStorageProjectReactor<InternalFileKey>): Mono<Unit> =
            keysToLoadFromClasspath.toFlux()
                .flatMap {
                    underlying.uploadFromClasspath(it)
                }
                .last()

    override fun doBuildKeyFromSuffix(s3KeySuffix: String): InternalFileKey {
        val (version, name) = s3KeySuffix.s3KeyToParts()
        return InternalFileKey(
            name = name,
            version = version,
        )
    }

    override fun doBuildS3KeySuffix(key: InternalFileKey): String = concatS3Key(key.version, key.name)
}
