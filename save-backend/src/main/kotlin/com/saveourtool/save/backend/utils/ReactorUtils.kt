/**
 * Utility methods for working with Reactor publishers
 */

package com.saveourtool.save.backend.utils

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toFlux
import java.nio.ByteBuffer
import java.util.Optional

/**
 * Same as [Flux.filter], but calls [onExclude] for every value not matching [predicate]
 *
 * @param onExclude
 * @param predicate
 * @return same as [Flux.filter]
 */
inline fun <T> Flux<T>.filterAndInvoke(crossinline onExclude: (T) -> Unit, crossinline predicate: (T) -> Boolean): Flux<T> = filter { value ->
    predicate(value).also {
        if (!it) {
            onExclude(value)
        }
    }
}

/**
 * Same as [Flux.filterWhen], but calls [onExclude] for every value not matching async [predicate]
 *
 * @param onExclude
 * @param predicate
 * @return Same as [Flux.filterWhen]
 */
inline fun <T> Flux<T>.filterWhenAndInvoke(crossinline onExclude: (T) -> Unit, crossinline predicate: (T) -> Mono<Boolean>): Flux<T> = filterWhen { value ->
    predicate(value).doOnNext {
        if (!it) {
            onExclude(value)
        }
    }
}

/**
 * @param objectMapper
 * @return convert current object to [Flux] of [ByteBuffer] as Json string using [objectMapper]
 */
fun <T> T.toFluxByteBufferAsJson(objectMapper: ObjectMapper): Flux<ByteBuffer> = Mono.fromCallable { objectMapper.writeValueAsBytes(this) }
    .map { ByteBuffer.wrap(it) }
    .toFlux()

/**
 * @param data
 * @param message
 * @return [Mono] containing [data] or [Mono.error] with 404 status otherwise
 */
fun <T> justOrNotFound(data: Optional<T>, message: String? = null) = Mono.justOrEmpty(data)
    .switchIfEmpty {
        Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, message))
    }
