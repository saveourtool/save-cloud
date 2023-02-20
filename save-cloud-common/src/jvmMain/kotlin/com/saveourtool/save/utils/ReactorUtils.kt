/**
 * Utility methods for working with Reactor publishers
 */

package com.saveourtool.save.utils

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream
import org.jetbrains.annotations.NonBlocking
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.switchIfEmptyDeferred
import reactor.kotlin.core.publisher.toMono

import java.io.InputStream
import java.io.SequenceInputStream
import java.nio.ByteBuffer

import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.reactor.asCoroutineDispatcher
import kotlinx.coroutines.reactor.asMono

@Suppress("WRONG_WHITESPACE")
private val logger = getLogger({}.javaClass)

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
 * Transforms the [left][Pair.first] value of each element of this [Flux].
 *
 * @param transformLeft the mapper function.
 * @return the transformed [Flux].
 * @see Flux.map
 */
fun <A : Any, B : Any, R : Any> Flux<Pair<A, B>>.mapLeft(transformLeft: (A, B) -> R): Flux<Pair<R, B>> =
        map { (left, right) ->
            transformLeft(left, right) to right
        }

/**
 * Transforms the [left][Pair.first] value of each element of this [Flux].
 *
 * @param transformLeft the mapper function.
 * @return the transformed [Flux].
 * @see Flux.map
 */
fun <A : Any, B : Any, R : Any> Flux<Pair<A, B>>.mapLeft(transformLeft: (A) -> R): Flux<Pair<R, B>> =
        mapLeft { left, _ ->
            transformLeft(left)
        }

/**
 * Transforms the [right][Pair.second] value of each element of this [Flux].
 *
 * @param transformRight the mapper function.
 * @return the transformed [Flux].
 * @see Flux.map
 */
fun <A : Any, B : Any, R : Any> Flux<Pair<A, B>>.mapRight(transformRight: (A, B) -> R): Flux<Pair<A, R>> =
        map { (first, second) ->
            first to transformRight(first, second)
        }

/**
 * Transforms the [right][Pair.second] value of each element of this [Flux].
 *
 * @param transformRight the mapper function.
 * @return the transformed [Flux].
 * @see Flux.map
 */
fun <A : Any, B : Any, R : Any> Flux<Pair<A, B>>.mapRight(transformRight: (B) -> R): Flux<Pair<A, R>> =
        mapRight { _, right ->
            transformRight(right)
        }

/**
 * @return collected [Flux] of [ByteBuffer] to [Mono] of [InputStream]
 */
fun Flux<ByteBuffer>.collectToInputStream(): Mono<InputStream> = this
    // take simple implementation from Jackson library
    .map { ByteBufferBackedInputStream(it) }
    .cast(InputStream::class.java)
    .reduce { in1, in2 ->
        SequenceInputStream(in1, in2)
    }

/**
 * @param keyExtractor the function used to extract the [Comparable] sort key
 * @return sorted original [Flux]
 */
fun <T : Any, K : Comparable<K>> Flux<T>.sortBy(keyExtractor: (T) -> K): Flux<T> = sort(Comparator.comparing(keyExtractor))

/**
 * A [ResponseSpec.bodyToMono] version which requests [Mono.subscribeOn] using
 * [Schedulers.boundedElastic].
 *
 * @return a [Mono] requesting asynchronously.
 * @see ResponseSpec.bodyToMono
 * @see Mono.subscribeOn
 * @see Schedulers.boundedElastic
 * @see blockingToMono
 * @see blockingToFlux
 * @see ResponseSpec.blockingToBodilessEntity
 */
@NonBlocking
inline fun <reified T : Any> ResponseSpec.blockingBodyToMono(): Mono<T> =
        bodyToMono<T>()
            .subscribeOn(Schedulers.boundedElastic())

/**
 * A [ResponseSpec.toBodilessEntity] version which requests [Mono.subscribeOn]
 * using [Schedulers.boundedElastic].
 *
 * @return a [Mono] requesting asynchronously.
 * @see ResponseSpec.toBodilessEntity
 * @see Mono.subscribeOn
 * @see Schedulers.boundedElastic
 * @see blockingToMono
 * @see blockingToFlux
 * @see ResponseSpec.blockingBodyToMono
 */
@NonBlocking
fun ResponseSpec.blockingToBodilessEntity(): Mono<EmptyResponse> =
        toBodilessEntity()
            .subscribeOn(Schedulers.boundedElastic())

/**
 * Taking from https://projectreactor.io/docs/core/release/reference/#faq.wrap-blocking
 *
 * @param supplier blocking operation like JDBC
 * @return [Mono] from result of blocking operation [T]
 * @see blockingToFlux
 * @see ResponseSpec.blockingBodyToMono
 * @see ResponseSpec.blockingToBodilessEntity
 */
@NonBlocking
fun <T : Any> blockingToMono(supplier: () -> T?): Mono<T> = supplier.toMono()
    .subscribeOn(Schedulers.boundedElastic())

/**
 * @param supplier blocking operation like JDBC
 * @return [Flux] from result of blocking operation [List] of [T]
 * @see blockingToMono
 * @see ResponseSpec.blockingBodyToMono
 * @see ResponseSpec.blockingToBodilessEntity
 */
@NonBlocking
fun <T> blockingToFlux(supplier: () -> Iterable<T>): Flux<T> = blockingToMono(supplier).flatMapIterable { it }

/**
 * @param interval how long to wait between checks
 * @param numberOfChecks how many times to check [checking]
 * @param checking action which checks that waiting can be finished
 * @return true if [checking] was successful before timeout, otherwise -- false
 */
fun waitReactivelyUntil(
    interval: Duration,
    numberOfChecks: Long,
    checking: () -> Boolean,
): Mono<Boolean> = Flux.interval(interval.toJavaDuration())
    .take(numberOfChecks)
    .map { checking() }
    .takeUntil { it }
    // check whether we have got `true` or Flux has completed with only `false`
    .any { it }

/**
 * Downloads the resource named [resourceName] from the classpath.
 *
 * @param resourceName the name of the resource (file).
 * @param lazyResponseBody the body of HTTP response if HTTP 404 is returned.
 * @return either the Mono holding the resource, or [Mono.error] with an HTTP 404
 *   status and response.
 */
fun downloadFromClasspath(
    resourceName: String,
    lazyResponseBody: (() -> String?) = { null },
): Mono<out Resource> =
        Mono.just(resourceName)
            .map(::ClassPathResource)
            .filter(Resource::exists)
            .switchIfEmptyToNotFound {
                logger.error("$resourceName is not found on the classpath; returning HTTP 404...")
                lazyResponseBody()
            }
