package com.saveourtool.save.cosv.processor

import com.saveourtool.save.cosv.storage.CosvStorage
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
    cosvStorage: CosvStorage,
) : AbstractCosvProcessor<JsonObject, JsonObject, JsonObject, JsonObject>(cosvStorage, serializer()) {
    override val id: String = ID

    override fun VulnerabilityDto.updateBySpecificFields(osv: RawOsvSchema): VulnerabilityDto = this

    companion object {
        /**
         * Identifier for [DefaultCosvProcessor]
         */
        const val ID = "default"
    }
}
