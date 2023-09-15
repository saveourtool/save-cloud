package com.saveourtool.save.cosv.processor

import com.saveourtool.save.cosv.utils.toJsonArrayOrSingle
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer
import org.springframework.stereotype.Component
import java.io.InputStream
import com.saveourtool.osv4k.RawOsvSchema as RawCosvSchema

/**
 * Processor of COSV entry which saves provided entry in saveourtool platform.
 */
@Component
class CosvProcessor {
    private val rawSerializer: KSerializer<RawCosvSchema> = serializer()

    /**
     * @param inputStream content of raw COSV file
     * @return list of [RawCosvSchema]
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun decode(
        inputStream: InputStream,
    ): List<RawCosvSchema> = Json.decodeFromStream<JsonElement>(inputStream)
        .toJsonArrayOrSingle()
        .map { jsonElement ->
            Json.decodeFromJsonElement(rawSerializer, jsonElement)
        }
}
