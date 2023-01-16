/**
 * This class contains util methods for Spring
 */

package com.saveourtool.save.utils

import com.saveourtool.save.spring.entity.BaseEntity
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.storage.Storage
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.codec.multipart.Part
import reactor.core.publisher.Mono

/**
 * upload with [Part] as content
 *
 * @param key a key for provided content
 * @param content
 * @return count of written bytes
 */
fun <K> Storage<K>.upload(key: K, content: Part): Mono<Long> = content.content()
    .map { it.asByteBuffer() }
    .let { upload(key, it) }

/**
 * upload with [Mono] of [Part] as content
 *
 * @param key a key for provided content
 * @param contentMono
 * @return count of written bytes
 */
fun <K> Storage<K>.upload(key: K, contentMono: Mono<Part>): Mono<Long> = contentMono
    .flatMap { upload(key, it) }

/**
 * overwrite with [Part] as content
 *
 * @param key a key for provided content
 * @param content
 * @return count of written bytes
 */
fun <K> Storage<K>.overwrite(key: K, content: Part): Mono<Long> = content.content()
    .map { it.asByteBuffer() }
    .let { overwrite(key, it) }

/**
 * overwrite with [Mono] of [Part] as content
 *
 * @param key a key for provided content
 * @param contentMono
 * @return count of written bytes
 */
fun <K> Storage<K>.overwrite(key: K, contentMono: Mono<Part>): Mono<Long> = contentMono
    .flatMap { overwrite(key, it) }

/**
 * @receiver repository for [T]
 * @param id ID of [T]
 * @return [T] found by [id] in [R] or response exception with status [org.springframework.http.HttpStatus.NOT_FOUND]
 */
inline fun <reified T : BaseEntity, R : BaseEntityRepository<T>> R.getByIdOrNotFound(id: Long): T = findByIdOrNull(id).orNotFound {
    "Not found ${T::class.simpleName} by id = $id"
}
