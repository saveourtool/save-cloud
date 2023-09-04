package com.saveourtool.save.cosv.processor

import com.saveourtool.save.cosv.repository.CosvRepository
import com.saveourtool.save.entities.vulnerability.VulnerabilityDto

import com.saveourtool.osv4k.RawOsvSchema
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

    companion object {
        /**
         * Identifier for [DefaultCosvProcessor]
         */
        const val ID = "default"
    }
}
