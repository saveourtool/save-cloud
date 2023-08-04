package com.saveourtool.save.osv.processor

import com.saveourtool.save.entities.vulnerability.VulnerabilityDto

import com.saveourtool.osv4k.OsvSchema
import kotlinx.serialization.json.JsonObject
import reactor.core.publisher.Mono

// TODO: need to move to osv4k library
typealias AnyOsvSchema = OsvSchema<out Any, out Any, out Any, out Any>

/**
 * Processor of OSV entry which creates [VulnerabilityDto].
 *  to save required info in save database
 */
interface OsvProcessor {
    /**
     * @param jsonObject should contain a single object only
     * @return [VulnerabilityDto]
     */
    fun apply(jsonObject: JsonObject): Mono<VulnerabilityDto>

    interface Custom : OsvProcessor {
        /**
         * @param id OSV id
         * @return true if [OsvProcessor] support this id
         */
        fun supports(id: String): Boolean
    }
}
