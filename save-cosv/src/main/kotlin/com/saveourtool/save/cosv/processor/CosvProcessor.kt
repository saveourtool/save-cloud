package com.saveourtool.save.cosv.processor

import com.saveourtool.save.cosv.repository.CosvSchema
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.CosvMetadataDto

import reactor.core.publisher.Mono

import kotlinx.serialization.json.JsonObject

// TODO: need to move to osv4k library
typealias AnyCosvSchema = CosvSchema<out Any, out Any, out Any, out Any>

/**
 * Processor of COSV entry which saves provided entry in saveourtool platform.
 */
interface CosvProcessor {
    /**
     * Identifier of [CosvProcessor] which provided by user
     */
    val id: String

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
    ): Mono<CosvMetadataDto>
}
