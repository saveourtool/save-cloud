package com.saveourtool.save.orchestrator.service

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.saveourtool.save.utils.sortBy
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.ProxyProvider
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.time.format.SignStyle
import java.time.temporal.ChronoField

/**
 * AgentLogService from Loki
 */
class LokiAgentLogService(
    lokiServiceUrl: String,
) : AgentLogService {
    private val webClient = WebClient.create(lokiServiceUrl)
        .mutate()
        .clientConnector(
            ReactorClientHttpConnector(HttpClient.create()
                .proxy { proxy ->
                    proxy.type(ProxyProvider.Proxy.HTTP)
                        .host("proxy.huawei.com")
                        .port(8080)
                }
            )
        )
        .build()

    override fun get(containerName: String, from: Instant, to: Instant): Mono<List<String>> {
        return webClient.get()
            .uri(
                "/loki/api/v1/query_range?query={query}&start={start}&end={end}&direction=forward",
                "{container_name=\"$containerName\"}",
                from.toEpochNanoStr(),
                to.toEpochNanoStr(),
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
                    .fromEpochNanoStr()
                    .let {
                        LocalDateTime.ofInstant(it, ZONE_ID)
                    }
                val msg = elements.next().asText()
                timestamp to msg
            }
            .sortBy { (timestamp, _) -> timestamp }
            .map { (timestamp, msg) ->
                "${DATE_TIME_FORMATTER.format(timestamp)} $msg"
            }
            .collectList()
    }

    private fun Instant.toEpochNanoStr(): String = "$epochSecond$nano"

    private fun String.fromEpochNanoStr(): Instant {
        val epochSecond = substring(0, length - 9)
        val nano = substring(length - 9)
        return Instant.ofEpochSecond(epochSecond.toLong(), nano.toLong())
    }

    companion object {
        private val ZONE_ID = ZoneOffset.UTC
        private val DATE_TIME_FORMATTER = DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral(' ')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendFraction(ChronoField.NANO_OF_SECOND, 9, 9, true)
            .toFormatter()
    }
}