package com.saveourtool.save.cosv.processor

import com.saveourtool.save.cosv.repository.CosvRepository
import com.saveourtool.save.cosv.repository.CosvSchemaKSerializer
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.CosvMetadataDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import reactor.core.publisher.Mono

/**
 * Default implementation of [CosvProcessor] which uses only core fields
 */
@Suppress("GENERIC_NAME")
abstract class AbstractCosvProcessor<D : Any, A_E : Any, A_D : Any, A_R_D : Any>(
    private val cosvRepository: CosvRepository,
    private val serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>,
) : CosvProcessor {
    override fun process(
        jsonObject: JsonObject,
        user: User,
        organization: Organization,
    ): Mono<CosvMetadataDto> {
        val osv = Json.decodeFromJsonElement(serializer, jsonObject)
        return cosvRepository.save(osv, serializer, user, organization)
    }
}
