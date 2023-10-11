package com.saveourtool.save.utils

import com.saveourtool.save.utils.http.HttpHeader
import com.saveourtool.save.utils.http.ServerTimingHttpHeader

/**
 * Adds support for headers.
 *
 * @property response the HTTP response.
 * @property headers the headers.
 * @see ResponseWithHeaders.Companion.invoke
 */
data class ResponseWithHeaders<T : Any>(
    val response: T,
    val headers: Array<out HttpHeader>
) {
    init {
        require(headers.any { it is ServerTimingHttpHeader }) {
            "At least one timing required"
        }
    }

    override fun equals(other: Any?): Boolean =
            other is ResponseWithHeaders<*> &&
                    response == other.response &&
                    headers contentEquals other.headers

    override fun hashCode(): Int =
            response.hashCode() xor headers.contentHashCode()

    companion object {
        /**
         * Adds support for the headers.
         *
         * @param response the HTTP response.
         * @param headers the headers.
         * @return the [response] with [headers] added.
         */
        operator fun <T : Any> invoke(
            response: T,
            vararg headers: HttpHeader,
        ): ResponseWithHeaders<T> =
                ResponseWithHeaders(response, headers)
    }
}
