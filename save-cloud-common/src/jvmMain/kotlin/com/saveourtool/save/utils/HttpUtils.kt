@file:JvmName("HttpUtils")
@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.utils

import com.saveourtool.save.utils.http.HttpHeader
import com.saveourtool.save.utils.http.ServerTimingHttpHeader
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.system.measureNanoTime
import kotlin.time.Duration.Companion.nanoseconds

/**
 * Lazy HTTP response.
 */
typealias LazyResponse<T> = () -> T

/**
 * Lazy HTTP response with headers.
 */
typealias LazyResponseWithHeaders<T> = () -> ResponseWithHeaders<T>

/**
 * Adds support for the required headers.
 */
private fun <T : Any> T.withHeaders(vararg headers: HttpHeader): ResponseWithHeaders<T> =
        ResponseWithHeaders(this, *headers)

/**
 * Adds support for the
 * [`Server-Timing`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Server-Timing)
 * header.
 */
private fun <T : Any> LazyResponse<T>.withHeaders(vararg headers: HttpHeader): LazyResponseWithHeaders<T> =
        {
            val response: T
            val nanos = measureNanoTime {
                response = this()
            }

            when {
                headers.none { it is ServerTimingHttpHeader } -> response.withHeaders(
                    *headers,
                    ServerTimingHttpHeader(
                        ServerTiming(
                            "total",
                            "Total server time",
                            nanos.nanoseconds
                        )
                    )
                )

                else -> response.withHeaders(*headers)
            }
        }

/**
 * @param headers the headers.
 * @param lazyResponse the lazy HTTP response.
 * @return [lazyResponse] wrapped with HTTP headers.
 */
@OptIn(ExperimentalContracts::class)
fun <T : Any> withHttpHeaders(
    vararg headers: HttpHeader,
    lazyResponse: LazyResponse<T>,
): ResponseEntity<T> {
    contract {
        callsInPlace(lazyResponse, EXACTLY_ONCE)
    }

    return withHttpHeaders(lazyResponse.withHeaders(*headers))
}

/**
 * Evaluates lazyResponse and returns an `HTTP 200 OK` with `Cache-Control` and
 * optional `Server-Timing` headers.
 *
 * @return [lazyResponse] wrapped with HTTP headers.
 */
@OptIn(ExperimentalContracts::class)
private fun <T : Any> withHttpHeaders(
    lazyResponse: LazyResponseWithHeaders<T>,
): ResponseEntity<T> {
    contract {
        callsInPlace(lazyResponse, EXACTLY_ONCE)
    }

    val response = lazyResponse()

    return ResponseEntity(
        response.response,
        httpHeaders(*response.headers),
        OK,
    )
}

/**
 * @return HTTP headers with `Cache-Control` and optional `Server-Timing`.
 */
private fun httpHeaders(vararg headers: HttpHeader): HttpHeaders =
        httpHeaders { headersBuilder ->
            headers.forEach { header ->
                headersBuilder[header.name] = header.value
            }
        }

/**
 * @return HTTP headers initialized with [init].
 */
@OptIn(ExperimentalContracts::class)
private fun httpHeaders(init: (headers: HttpHeaders) -> Unit): HttpHeaders {
    contract {
        callsInPlace(init, EXACTLY_ONCE)
    }

    return HttpHeaders().also { headers ->
        init(headers)
    }
}
