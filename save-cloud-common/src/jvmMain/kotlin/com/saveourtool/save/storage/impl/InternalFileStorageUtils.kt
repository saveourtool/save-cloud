/**
 * Utils methods for storages with key **InternalFileKey**
 */

package com.saveourtool.save.storage.impl

import com.saveourtool.save.storage.StorageCoroutines
import com.saveourtool.save.storage.StorageProjectReactor
import com.saveourtool.save.utils.downloadFromClasspath
import com.saveourtool.save.utils.getFromClasspath
import com.saveourtool.save.utils.toByteBufferFlux

import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

import kotlinx.coroutines.reactive.asFlow

/**
 * Upload [key] from classpath to storage ([this])
 *
 * @param key
 * @return [Mono] without body
 */
fun StorageProjectReactor<InternalFileKey>.uploadFromClasspath(key: InternalFileKey): Mono<Unit> = doesExist(key)
    .filterWhen { exists ->
        if (exists && key.isLatest()) {
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
    .thenReturn(Unit)
    .defaultIfEmpty(Unit)

/**
 * Upload [key] from classpath to storage ([this])
 *
 * @param key
 */
suspend fun StorageCoroutines<InternalFileKey>.uploadFromClasspath(key: InternalFileKey) {
    val needsToUpload = if (!doesExist(key)) {
        true
    } else if (key.isLatest()) {
        delete(key)
    } else {
        false
    }
    if (needsToUpload) {
        val resource = getFromClasspath(key.name) {
            "Can't find ${key.name}"
        }
        upload(key, resource.contentLength(), resource.toByteBufferFlux().asFlow())
    }
}
