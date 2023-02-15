package com.saveourtool.save.storage.impl

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.AbstractS3Storage
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.storage.s3KeyToPartsTill
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
abstract class AbstractInternalFileStorage(
    private val keysToLoadFromClasspath: Collection<InternalFileKey>,
    s3StoragePrefix: String,
    s3Operations: S3Operations,
) : AbstractS3Storage<InternalFileKey>(
    s3Operations,
    concatS3Key(s3StoragePrefix, "internal-storage")
) {
    /**
     * An init method to upload internal files to S3 from classpath or github
     *
     * @return [Mono] without body
     */
    override fun doInitAsync(): Mono<Unit> = Flux.concat(
        keysToLoadFromClasspath.toFlux()
            .flatMap {
                uploadFromClasspath(it, it.isLatest())
            },
        doInitAdditionally(),
    )
        .last()

    /**
     * Async method to init this storage: copy required files to storage
     *
     * @return [Mono] without body
     */
    protected open fun doInitAdditionally(): Mono<Unit> = Mono.just(Unit)

    override fun buildKey(s3KeySuffix: String): InternalFileKey {
        val (version, name) = s3KeySuffix.s3KeyToPartsTill(prefix)
        return InternalFileKey(
            name = name,
            version = version,
        )
    }

    override fun buildS3KeySuffix(key: InternalFileKey): String = concatS3Key(key.version, key.name)

    private fun uploadFromClasspath(key: InternalFileKey, overwrite: Boolean): Mono<Unit> = doesExist(key)
        .filterWhen { exists ->
            if (exists && overwrite) {
                delete(key)
            } else {
                exists.not().toMono()
            }
        }
        .flatMap {
            downloadFromClasspath(key.name) {
                "Can't find ${key.name}"
            }
                .flatMap { resource ->
                    upload(
                        key,
                        resource.contentLength(),
                        resource.toByteBufferFlux(),
                    )
                }
        }
        .defaultIfEmpty(Unit)
}
