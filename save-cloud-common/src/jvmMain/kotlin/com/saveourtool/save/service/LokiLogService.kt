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
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

/**
 * AgentLogService from Loki
 */
class LokiLogService(
    lokiServiceUrl: String,
) : LogService {
    private val webClient = WebClient.create(lokiServiceUrl)
        .let {
            if (System.getProperty("ENABLE_HUAWEI_PROXY").toBoolean()) {
                it.enableHuaweiProxy()
            } else {
                it
            }
        }

    override fun get(containerName: String, from: Instant, to: Instant): Mono<StringList> {
        val query = "{container_name=\"$containerName\"}"
        return doQueryRange(query, from, to, null)
            .expand { previousLokiBatch ->
                doQueryRange(query, from, to, previousLokiBatch)
            }
            .flatMapIterable { it.entries.map(LogEntry::toString) }
            .collectList()
    }

    private fun doQueryRange(
        query: String,
        originalStart: Instant,
        end: Instant,
        previousLokiBatch: LokiBatch?,
    ): Mono<LokiBatch> = webClient.get()
        .uri(
            "/loki/api/v1/query_range?query={query}&start={start}&end={end}&direction=forward&limit={limit}",
            query,
            (previousLokiBatch?.lastEntries?.firstOrNull()?.timestamp ?: originalStart).toEpochNanoStr(),
            end.toEpochNanoStr(),
            LOKI_LIMIT,
        )
        .retrieve()
        .bodyToMono<ObjectNode>()
        .map { it.extractResult(previousLokiBatch?.lastEntries.orEmpty()) }
        .filter(LokiBatch::isEmpty)

    private fun ObjectNode.extractResult(logEntriesToSkip: Set<LogEntry>): LokiBatch = this
        .validateFieldValue("status", "success")
        .get("data")
        .validateFieldValue("resultType", "streams")
        .elementsAsSequenceFrom("result")
        .flatMap { it.elementsAsSequenceFrom("values") }
        .map { jsonNode ->
            val (timestampNode, msgNode) = jsonNode.elementsAsSequence().toList()
                .also { child ->
                    require(child.size == 2) {
                        "Only two values are expected in each elements in [values]"
                    }
                }
            val timestamp = timestampNode.asText()
                .fromEpochNanoStr()
            val msg = msgNode.asText()
            LogEntry(timestamp, msg)
        }
        .filterNot(logEntriesToSkip::contains)
        .sorted()  // it's not clear why loki/logcli sorts output -- so we will sort too
        .toList()
        .let { LokiBatch(it) }

    private fun JsonNode.validateFieldValue(fieldName: String, expectedValue: String): JsonNode = also { jsonNode ->
        val actualValue = jsonNode[fieldName].asText()
        require(actualValue == expectedValue) {
            "Invalid $fieldName: $actualValue"
        }
    }

    private fun JsonNode.elementsAsSequence(): Sequence<JsonNode> = (this as ArrayNode).elements().asSequence()

    private fun JsonNode.elementsAsSequenceFrom(fieldName: String): Sequence<JsonNode> = (this[fieldName] as ArrayNode)
        .elementsAsSequence()

    private fun Instant.toEpochNanoStr(): String = "$epochSecond${nano.toString().padStart(NANO_COUNT, '0')}"

    private fun String.fromEpochNanoStr(): Instant {
        val epochSecond = substring(0, length - NANO_COUNT)
        val nano = substring(length - NANO_COUNT)
        return Instant.ofEpochSecond(epochSecond.toLong(), nano.toLong())
    }

    /**
     * @property entries
     */
    private data class LokiBatch(
        val entries: List<LogEntry>,
    ) {
        val lastEntries: Set<LogEntry> = entries
            .lastOrNull()
            ?.timestamp
            ?.let { lastTimestamp -> entries.filterTo(HashSet()) { it.timestamp == lastTimestamp } }
            ?.also { result ->
                require(result.size < LOKI_LIMIT) {
                    "Need to increase limit in request to loki"
                }
            }
            .orEmpty()

        /**
         * @return true if there are no [entries]
         */
        fun isEmpty(): Boolean = entries.isEmpty()
    }

    /**
     * @property timestamp
     * @property msg
     */
    private data class LogEntry(
        val timestamp: Instant,
        val msg: String,
    ) : Comparable<LogEntry> {
        override fun compareTo(other: LogEntry): Int = timestamp.compareTo(other.timestamp)

        override fun toString(): String = "${dateTimeFormatter.format(timestamp.atZone(ZoneId.systemDefault()))} $msg"
    }

    companion object {
        private const val LOKI_LIMIT = 100
        private const val NANO_COUNT = 9
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
