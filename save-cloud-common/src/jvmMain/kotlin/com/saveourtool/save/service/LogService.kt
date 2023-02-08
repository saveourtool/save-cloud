package com.saveourtool.save.service

import com.saveourtool.save.utils.StringList
import reactor.core.publisher.Mono
import java.time.Instant

/**
 * A service which provides logs from container or application
 */
interface LogService {
    /**
     * @param containerName name of container
     * @param from query is from this timestamp in UTC
     * @param to query is to this timestamp in UTC
     * @param limit limit for result
     * @return logs as [Mono] of [String]s
     */
    fun getByContainerName(
        containerName: String,
        from: Instant,
        to: Instant,
        limit: Int,
    ): Mono<StringList>

    /**
     * @param applicationName name of application
     * @param from query is from this timestamp in UTC
     * @param to query is to this timestamp in UTC
     * @param limit limit for result
     * @return logs as [Mono] of [String]s
     */
    fun getByApplicationName(
        applicationName: String,
        from: Instant,
        to: Instant,
        limit: Int,
    ): Mono<StringList>

    companion object {
        /**
         * Stub implementation of [LogService]
         */
        val stub: LogService = object : LogService {
            override fun getByContainerName(
                containerName: String,
                from: Instant,
                to: Instant,
                limit: Int,
            ): Mono<StringList> = Mono.just(
                listOf("Stub implementation: requested logs by container name for $containerName from $from to $to with limit $limit")
            )

            override fun getByApplicationName(
                applicationName: String,
                from: Instant,
                to: Instant,
                limit: Int,
            ): Mono<StringList> = Mono.just(
                listOf("Stub implementation: requested logs by application name for $applicationName from $from to $to with limit $limit")
            )
        }
    }
}
