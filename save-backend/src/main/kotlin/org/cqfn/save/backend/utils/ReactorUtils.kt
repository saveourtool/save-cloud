package org.cqfn.save.backend.utils

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.util.Optional

/**
 * @param onExclude
 * @param predicate
 * @return
 */
inline fun <T> Flux<T>.filterAndInvoke(crossinline onExclude: (T) -> Unit, crossinline predicate: (T) -> Boolean): Flux<T> = filter { value ->
    predicate(value).also {
        if (!it) {
            onExclude(value)
        }
    }
}

/**
 * @param onExclude
 * @param predicate
 * @return
 */
inline fun <T> Flux<T>.filterWhenAndInvoke(crossinline onExclude: (T) -> Unit, crossinline predicate: (T) -> Mono<Boolean>): Flux<T> = filterWhen { value ->
    predicate(value).doOnNext {
        if (!it) {
            onExclude(value)
        }
    }
}

/**
 * @param data
 * @param message
 * @return
 */
fun <T> justOrNotFound(data: Optional<T>, message: String? = null) = Mono.justOrEmpty(data)
    .switchIfEmpty {
        Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, message))
    }
