package com.saveourtool.save.service

import reactor.core.publisher.Flux
import java.time.Instant

/**
 * A service which provides container's logs
 */
interface LogService {
    /**
     * @param containerName name of container
     * @param from query is from this timestamp in UTC
     * @param to query is to this timestamp in UTC
     * @return logs as [Flux] of [String]
     */
    fun get(containerName: String, from: Instant, to: Instant): Flux<String>

    companion object {
        /**
         * Stub implementation of [LogService]
         */
        val STUB: LogService = object : LogService {
            override fun get(containerName: String, from: Instant, to: Instant): Flux<String> = Flux.just(
                "Stub implementation: requested logs for $containerName from $from to $to"
            )
        }
    }
}
