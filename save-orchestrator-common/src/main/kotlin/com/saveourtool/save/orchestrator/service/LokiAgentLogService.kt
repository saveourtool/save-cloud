package com.saveourtool.save.orchestrator.service

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.time.Instant
import java.util.Comparator

/**
 *
 */
class LokiAgentLogService(
    lokiServiceUrl: String,
) : AgentLogService {
    private val webClient = WebClient.create(lokiServiceUrl)

    override fun get(containerName: String, from: Instant, to: Instant): Mono<List<String>> {
        return webClient.get()
            .uri(
                "/loki/api/v1/query_range?query={query}&start={start}&end={end}&direction=forward",
                "container_name=$containerName",
                from.epochNanoStr(),
                to.epochNanoStr(),
            )
            .retrieve()
            .bodyToMono<ObjectNode>()
            .filter {
                it["status"].asText() == "success"
            }
            .flatMapMany {
                (it["data"]["result"] as ArrayNode).elements()
                    .toFlux()
            }
            .flatMap {
                (it["values"] as ArrayNode).elements()
                    .toFlux()
            }
            .map { jsonNode ->
                val elements = (jsonNode as ArrayNode).elements()
                val timestamp = elements.next().asText()
                val msg = elements.next().asText()
                timestamp to msg
            }
            .sort(Comparator.comparing(Pair<String, String>::first))
            .map(Pair<String, String>::second)
            .collectList()
    }

    private fun Instant.epochNanoStr(): String = "$epochSecond$nano"
}