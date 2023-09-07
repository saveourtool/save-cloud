package com.saveourtool.save.cosv.processor

import com.saveourtool.save.cosv.repository.CosvRepository
import com.saveourtool.save.cosv.repository.CosvSchemaKSerializer
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.CosvMetadataDto

import com.saveourtool.osv4k.RawOsvSchema
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer

/**
 * Processor of COSV entry which saves provided entry in saveourtool platform.
 */
@Component
class CosvProcessor(
    private val cosvRepository: CosvRepository,
) {
    private val rawSerializer: CosvSchemaKSerializer<JsonObject, JsonObject, JsonObject, JsonObject> = serializer()

    /**
     * @param jsonObject should contain a single object only
     * @param user who uploads
     * @param organization to which is uploaded
     * @return [CosvMetadataDto]
     */
    fun process(
        jsonObject: JsonObject,
        user: User,
        organization: Organization,
    ): Mono<CosvMetadataDto> {
        val osv = Json.decodeFromJsonElement(rawSerializer, jsonObject)
        return cosvRepository.save(osv, rawSerializer, user, organization)
    }
}
