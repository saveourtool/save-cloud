package com.saveourtool.save.cosv.processor

import com.saveourtool.save.cosv.repository.CosvRepository
import com.saveourtool.save.cosv.utils.toJsonArrayOrSingle
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.VulnerabilityMetadataDto

import com.saveourtool.osv4k.RawOsvSchema
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

import java.io.InputStream

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer

/**
 * Processor of COSV entry which saves provided entry in saveourtool platform.
 */
@Component
class CosvProcessor(
    private val cosvRepository: CosvRepository,
) {
    private val rawSerializer: KSerializer<RawOsvSchema> = serializer()

    /**
     * @param inputStream content of raw COSV file
     * @return list of [RawOsvSchema]
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun decode(
        inputStream: InputStream,
    ): List<RawOsvSchema> = Json.decodeFromStream<JsonElement>(inputStream)
        .toJsonArrayOrSingle()
        .map { jsonElement ->
            Json.decodeFromJsonElement(rawSerializer, jsonElement)
        }

    /**
     * @param cosv
     * @param user who uploads
     * @param organization to which is uploaded
     * @return [VulnerabilityMetadataDto]
     */
    fun save(
        cosv: RawOsvSchema,
        user: User,
        organization: Organization,
    ): Mono<VulnerabilityMetadataDto> = cosvRepository.save(cosv, rawSerializer, user, organization)

    companion object {
        /**
         * [KSerializer] for [RawOsvSchema]
         */
        val rawSerializer: KSerializer<RawOsvSchema> = serializer()
    }
}
