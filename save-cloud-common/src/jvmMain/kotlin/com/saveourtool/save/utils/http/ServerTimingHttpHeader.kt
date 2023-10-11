package com.saveourtool.save.utils.http

import com.saveourtool.save.utils.ServerTiming

/**
 * HttpHeader for "Server-Timing"
 *
 * @param timings
 * @see ServerTimingHttpHeader.Companion.invoke
 */
class ServerTimingHttpHeader(
    private val timings: Array<out ServerTiming>
) : HttpHeader {
    init {
        require(timings.isNotEmpty()) {
            "At least one timing required"
        }
    }

    override val name: String = "Server-Timing"
    override val value: String = timings.joinToString()
    override fun equals(other: Any?): Boolean =
        other is ServerTimingHttpHeader &&
                timings contentEquals other.timings

    override fun hashCode(): Int = timings.contentHashCode()


    companion object {
        /**
         * Adds support for the
         * [`Server-Timing`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Server-Timing)
         * header.
         *
         * @param timings the server-side timings.
         * @return the [HttpHeader] with [timings] added.
         */
        operator fun invoke(
            vararg timings: ServerTiming,
        ): ServerTimingHttpHeader =
            ServerTimingHttpHeader(timings)
    }
}