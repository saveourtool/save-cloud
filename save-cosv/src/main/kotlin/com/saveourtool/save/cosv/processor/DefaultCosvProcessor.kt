package com.saveourtool.save.cosv.processor

import com.saveourtool.save.cosv.repository.CosvRepository

import com.saveourtool.osv4k.RawOsvSchema
import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import org.springframework.stereotype.Component

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer

/**
 * Default implementation of [CosvProcessor] which uses only core fields
 */
@Component
class DefaultCosvProcessor(
    cosvRepository: CosvRepository,
) : AbstractCosvProcessor<JsonObject, JsonObject, JsonObject, JsonObject>(cosvRepository, serializer()) {
    override val id: String = ID

    override fun VulnerabilityDto.updateBySpecificFields(osv: RawOsvSchema): VulnerabilityDto = this

    companion object {
        /**
         * Identifier for [DefaultCosvProcessor]
         */
        const val ID = "default"
    }
}
