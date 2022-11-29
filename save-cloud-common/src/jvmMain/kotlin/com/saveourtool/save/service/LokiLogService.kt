package com.saveourtool.save.service

import com.saveourtool.save.utils.StringList
import com.saveourtool.save.utils.enableHuaweiProxy

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

/**
 * AgentLogService from Loki
 */
class LokiLogService(
    lokiServiceUrl: String,
    enableHuaweiProxy: Boolean = true,
) : LogService {
    private val webClient = WebClient.create(lokiServiceUrl)
        .let {
            if (enableHuaweiProxy) {
                it.enableHuaweiProxy()
            } else {
                it
            }
        }

    override fun get(containerName: String, from: Instant, to: Instant): Mono<StringList> = webClient.get()
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
        .map { objectNode ->
            objectNode["data"]
                .elementsAsSequenceFrom("result")
                .flatMap { it.elementsAsSequenceFrom("values") }
                .map { jsonNode ->
                    val (timestampNode, msgNode) = jsonNode.elementsAsSequence().toList()
                    val timestamp = timestampNode.asText()
                        .fromEpochNanoStr()
                        .let {
                            LocalDateTime.ofInstant(it, zoneId)
                        }
                    val msg = msgNode.asText()
                    timestamp to msg
                }
                .sortedBy { (timestamp, _) -> timestamp }
                .map { (timestamp, msg) ->
                    "${dateTimeFormatter.format(timestamp)} $msg"
                }
                .toList()
        }

    private fun doQueryRange(query: String, start: String, end: String): Mono<StringList> {
        return Mono.empty()
    }

    private fun

    private fun Instant.toEpochNanoStr(): String = "$epochSecond${nano.toString().padStart(NANO_COUNT, '0')}"

    private fun JsonNode.elementsAsSequence(): Sequence<JsonNode> = (this as ArrayNode).elements().asSequence()
    private fun JsonNode.elementsAsSequenceFrom(fieldName: String): Sequence<JsonNode> = (this[fieldName] as ArrayNode)
        .elementsAsSequence()

    private fun String.fromEpochNanoStr(): Instant {
        val epochSecond = substring(0, length - NANO_COUNT)
        val nano = substring(length - NANO_COUNT)
        return Instant.ofEpochSecond(epochSecond.toLong(), nano.toLong())
    }

    companion object {
        private const val NANO_COUNT = 9
        private val zoneId = ZoneOffset.UTC
        private val dateTimeFormatter = DateTimeFormatterBuilder()
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
