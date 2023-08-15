package com.saveourtool.save.osv.processor

import com.saveourtool.save.entities.vulnerability.VulnerabilityDto

import com.saveourtool.osv4k.OsvSchema
import reactor.core.publisher.Mono

import kotlinx.serialization.json.JsonObject

// TODO: need to move to osv4k library
typealias AnyOsvSchema = OsvSchema<out Any, out Any, out Any, out Any>

/**
 * Processor of OSV entry which creates [VulnerabilityDto].
 *  to save required info in save database
 */
interface OsvProcessor : Function1<JsonObject, Mono<VulnerabilityDto>> {
    /**
     * Identifier of [OsvProcessor] which provided by user
     */
    val id: String

    /**
     * @param jsonObject should contain a single object only
     * @return [VulnerabilityDto]
     */
    override fun invoke(jsonObject: JsonObject): Mono<VulnerabilityDto>
}
