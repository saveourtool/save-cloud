/**
 * Utility methods for working with Reactor publishers
 */

package com.saveourtool.save.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.switchIfEmptyDeferred
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.io.InputStream
import java.io.SequenceInputStream
import java.nio.ByteBuffer
import java.util.Comparator
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * @param status
 * @param messageCreator
 * @return original [Mono] or [Mono.error] with [status] otherwise
 */
fun <T> Mono<T>.switchIfEmptyToResponseException(status: HttpStatus, messageCreator: (() -> String?) = { null }) = switchIfEmpty {
    Mono.error(ResponseStatusException(status, messageCreator()))
}

/**
 * @param messageCreator
 * @return original [Mono] or [Mono.error] with 404 status otherwise
 */
fun <T> Mono<T>.switchIfEmptyToNotFound(messageCreator: (() -> String?) = { null }) = switchIfEmptyToResponseException(HttpStatus.NOT_FOUND, messageCreator)

/**
 * @param messageCreator
 * @return original [Flux] or [Mono.error] with 404 status otherwise
 */
fun <T> Flux<T>.switchIfEmptyToNotFound(messageCreator: (() -> String?) = { null }) = switchIfEmptyDeferred {
    Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, messageCreator()))
}

/**
 * @param predicate
 * @param status
 * @param messageCreator
 * @return original [Mono] or [Mono.error] with [status] if [predicate] is true for value in [Mono]
 */
@Suppress("LAMBDA_IS_NOT_LAST_PARAMETER")
fun <T> Mono<T>.requireOrSwitchToResponseException(
    predicate: T.() -> Boolean,
    status: HttpStatus,
    messageCreator: (() -> String?) = { null }
) = filter(predicate).switchIfEmptyToResponseException(status, messageCreator)

/**
 * @param lazyValue default value creator
 * @return original [Mono] with switch to default value if original [Mono] is empty
 */
fun <T> Mono<T>.lazyDefaultIfEmpty(lazyValue: () -> T): Mono<T> = switchIfEmpty {
    Mono.fromCallable(lazyValue)
}

/**
 * @param other other value which will be returned in [Mono]
 * @return [Mono] with [other] as value which will be returned after [Flux] receiver
 */
fun <T : Any> Flux<*>.thenJust(other: T): Mono<T> = then(Mono.just(other))

/**
 * Run [effect] and then return the original value
 *
 * @param effect
 * @return always returns [Mono] with the original value. Uses [Mono.flatMap] under the hood,
 * so all signals are treated accordingly.
 */
fun <T : Any> Mono<T>.asyncEffect(effect: (T) -> Mono<out Any>): Mono<T> = flatMap { value ->
    effect(value).thenReturn(value)
}

/**
 * If content of [this] [Mono] matches [predicate], run [effect].
 *
 * @param predicate
 * @param effect
 * @return always returns [Mono] with the original value. Uses [Mono.flatMap] under the hood,
 * so all signals are treated accordingly.
 */
fun <T : Any> Mono<T>.asyncEffectIf(predicate: T.() -> Boolean, effect: (T) -> Mono<out Any>): Mono<T> = asyncEffect { value ->
    if (predicate(value)) {
        effect(value)
    } else {
        Mono.just(Unit)
    }
}

/**
 * @return convert [Flux] of [ByteBuffer] to [Mono] of [InputStream]
 */
fun Flux<ByteBuffer>.mapToInputStream(): Mono<InputStream> = this
    // take simple implementation from Jackson library
    .map { ByteBufferBackedInputStream(it) }
    .cast(InputStream::class.java)
    .reduce { in1, in2 ->
        SequenceInputStream(in1, in2)
    }

/**
 * @param objectMapper
 * @return convert current object to [Flux] of [ByteBuffer] as Json string using [objectMapper]
 */
fun <T> T.toFluxByteBufferAsJson(objectMapper: ObjectMapper): Flux<ByteBuffer> = Mono.fromCallable { objectMapper.writeValueAsBytes(this) }
    .map { ByteBuffer.wrap(it) }
    .toFlux()

/**
 * @param keyExtractor the function used to extract the [Comparable] sort key
 * @return sorted original [Flux]
 */
fun <T : Any, K : Comparable<K>> Flux<T>.sortBy(keyExtractor: (T) -> K): Flux<T> = sort(Comparator.comparing(keyExtractor))

/**
 * Taking from https://projectreactor.io/docs/core/release/reference/#faq.wrap-blocking
 *
 * @param supplier blocking operation like JDBC
 * @return [Mono] from result of blocking operation [T]
 */
fun <T : Any> blockingToMono(supplier: () -> T?): Mono<T> = supplier.toMono()
    .subscribeOn(Schedulers.boundedElastic())

/**
 * @param supplier blocking operation like JDBC
 * @return [Flux] from result of blocking operation [List] of [T]
 */
fun <T> blockingToFlux(supplier: () -> Iterable<T>): Flux<T> = blockingToMono(supplier).flatMapIterable { it }

/**
 * @param till
 * @param times
 * @param checking
 * @return true if [checking] was successful before timeout, otherwise -- false
 */
fun waitReactively(till: Duration, times: Int, checking: () -> Boolean): Mono<Boolean> = Flux.interval((till / times).toJavaDuration())
    .take(times.toLong())
    .map {
        checking()
    }
    .takeUntil { it }
    // check whether we have got `true` or Flux has completed with only `false`
    .any { it }

/**
 * @param till
 * @param interval
 * @param checking
 * @return true if [checking] was successful before timeout, otherwise -- false
 */
fun waitReactively(till: Duration, interval: Duration, checking: () -> Boolean): Mono<Boolean> = waitReactively(interval, (till / interval).roundToInt(), checking)
