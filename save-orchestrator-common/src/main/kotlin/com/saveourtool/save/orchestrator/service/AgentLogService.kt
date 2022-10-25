package com.saveourtool.save.orchestrator.service

import reactor.core.publisher.Mono
import java.time.Instant

/**
 * A service which provides logs of agents
 */
interface AgentLogService {
    /**
     * @param containerName name of container\agent
     * @param from query is from this timestamp in UTC
     * @param to query is to this timestamp in UTC
     * @return logs as list of Strings
     */
    fun get(containerName: String, from: Instant, to: Instant): Mono<List<String>>
}
