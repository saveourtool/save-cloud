/**
 * Utils methods for storages with key **InternalFileKey**
 */

package com.saveourtool.save.storage.impl

import com.saveourtool.save.storage.StorageProjectReactor
import com.saveourtool.save.utils.downloadFromClasspath
import com.saveourtool.save.utils.toByteBufferFlux

import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

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
