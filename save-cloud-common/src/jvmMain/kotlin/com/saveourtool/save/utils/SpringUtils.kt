/**
 * This class contains util methods for Spring
 */

package com.saveourtool.save.utils

import com.saveourtool.save.spring.entity.BaseEntity
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.storage.Storage
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.codec.multipart.Part
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer

/**
 * upload [ByteArray] as content
 *
 * @param key a key for provided content
 * @param contentBytes
 * @return count of written bytes
 */
fun <K> Storage<K>.upload(key: K, contentBytes: ByteArray): Mono<Long> = contentBytes.size.toLong()
    .let { contentLength ->
        upload(key, contentLength, Flux.just(ByteBuffer.wrap(contentBytes))).thenReturn(contentLength)
    }

/**
 * overwrite with [Part] as content
 *
 * @param key a key for provided content
 * @param content
 * @param contentLength
 * @return count of written bytes
 */
fun <K> Storage<K>.overwrite(key: K, content: Part, contentLength: Long): Mono<Unit> = content.content()
    .map { it.asByteBuffer() }
    .let { overwrite(key, contentLength, it) }

/**
 * overwrite [ByteArray] as content
 *
 * @param key a key for provided content
 * @param contentBytes
 * @return count of written bytes
 */
fun <K> Storage<K>.overwrite(key: K, contentBytes: ByteArray): Mono<Long> = contentBytes.size.toLong()
    .let { contentLength ->
        overwrite(key, contentLength, Flux.just(ByteBuffer.wrap(contentBytes))).thenReturn(contentLength)
    }

/**
 * @receiver repository for [T]
 * @param id ID of [T]
 * @return [T] found by [id] in [R] or response exception with status [org.springframework.http.HttpStatus.NOT_FOUND]
 */
inline fun <reified T : BaseEntity, R : BaseEntityRepository<T>> R.getByIdOrNotFound(id: Long): T = findByIdOrNull(id).orNotFound {
    "Not found ${T::class.simpleName} by id = $id"
}
