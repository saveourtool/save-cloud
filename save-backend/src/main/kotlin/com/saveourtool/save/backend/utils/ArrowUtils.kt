@file:JvmName("ArrowUtils")
@file:Suppress(
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
    "UNUSED_IMPORT",  // False positive
)

package com.saveourtool.save.backend.utils

import arrow.core.Either
import arrow.core.getOrElse
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.web.server.ResponseStatusException

/**
 * Returns the value from this _Right_ or throws an exception.
 *
 * @param lazyException the exception to be thrown if this _Either_ is a _left_.
 * @return the value from this _Right_.
 * @see Either.getOrElse
 */
fun <T> Either<ErrorMessage, T>.getOrThrow(lazyException: (ErrorMessage) -> Throwable): T =
        getOrElse { error ->
            throw lazyException(error)
        }

/**
 * Returns the value from this _Right_ or throws a [ResponseStatusException].
 *
 * @param status the HTTP status to be reported to the client.
 * @return the value from this _Right_.
 * @see Either.getOrElse
 */
fun <T> Either<ErrorMessage, T>.getOrThrow(status: HttpStatus): T =
        getOrThrow { error ->
            ResponseStatusException(status, error.message)
        }

/**
 * Returns the value from this _Right_ or throws a [ResponseStatusException].
 *
 * @return the value from this _Right_.
 * @see Either.getOrElse
 */
fun <T> Either<ErrorMessage, T>.getOrThrowBadRequest(): T =
        getOrThrow(BAD_REQUEST)
