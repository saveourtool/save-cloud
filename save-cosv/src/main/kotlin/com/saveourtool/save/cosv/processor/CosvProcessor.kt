package com.saveourtool.save.cosv.processor

import com.saveourtool.save.entities.vulnerability.VulnerabilityDto

import com.saveourtool.osv4k.OsvSchema as CosvSchema
import reactor.core.publisher.Mono

import kotlinx.serialization.json.JsonObject

// TODO: need to move to osv4k library
typealias AnyCosvSchema = CosvSchema<out Any, out Any, out Any, out Any>

/**
 * Processor of COSV entry which creates [VulnerabilityDto].
 *  to save required info in save database
 */
interface CosvProcessor : Function1<JsonObject, Mono<VulnerabilityDto>> {
    /**
     * Identifier of [CosvProcessor] which provided by user
     */
    val id: String

    /**
     * @param jsonObject should contain a single object only
     * @return [VulnerabilityDto]
     */
    override fun invoke(jsonObject: JsonObject): Mono<VulnerabilityDto>
}
