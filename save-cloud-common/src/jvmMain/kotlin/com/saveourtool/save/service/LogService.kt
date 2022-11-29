package com.saveourtool.save.service

import com.saveourtool.save.utils.StringList
import reactor.core.publisher.Mono
import java.time.Instant

/**
 * A service which provides container's logs
 */
interface LogService {
    /**
     * @param containerName name of container
     * @param from query is from this timestamp in UTC
     * @param to query is to this timestamp in UTC
     * @return logs as [Mono] of [String]s
     */
    fun get(containerName: String, from: Instant, to: Instant): Mono<StringList>

    companion object {
        /**
         * Stub implementation of [LogService]
         */
        val stub: LogService = object : LogService {
            override fun get(containerName: String, from: Instant, to: Instant): Mono<StringList> = Mono.just(
                listOf("Stub implementation: requested logs for $containerName from $from to $to")
            )
        }
    }
}
