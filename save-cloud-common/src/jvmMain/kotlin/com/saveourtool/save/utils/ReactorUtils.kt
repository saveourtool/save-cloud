/**
 * Utility methods for working with Reactor publishers
 */

package com.saveourtool.save.utils

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.switchIfEmptyDeferred
import reactor.kotlin.core.publisher.toMono

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
 * If content of [this] [Mono] matches [predicate], run [effect].
 *
 * @param predicate
 * @param effect
 * @return always returns [Mono] with the original value. Uses [Mono.flatMap] under the hood,
 * so all signals are treated accordingly.
 */
fun <T : Any> Mono<T>.asyncEffectIf(predicate: T.() -> Boolean, effect: (T) -> Mono<out Any>): Mono<T> = flatMap { value ->
    if (predicate(value)) {
        effect(value).map { value }
    } else {
        Mono.just(value)
    }
}

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
