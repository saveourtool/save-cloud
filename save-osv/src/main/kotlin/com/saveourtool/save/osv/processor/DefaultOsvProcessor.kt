package com.saveourtool.save.osv.processor

import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.osv.storage.OsvStorage

import com.saveourtool.osv4k.RawOsvSchema
import org.springframework.stereotype.Component

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer

/**
 * Default implementation of [OsvProcessor] which uses only core fields
 */
@Component
class DefaultOsvProcessor(
    osvStorage: OsvStorage,
) : AbstractOsvProcessor<JsonObject, JsonObject, JsonObject, JsonObject>(osvStorage, serializer()) {
    override val id: String = ID

    override fun VulnerabilityDto.updateBySpecificFields(osv: RawOsvSchema): VulnerabilityDto = this

    companion object {
        /**
         * Identifier for [DefaultOsvProcessor]
         */
        const val ID = "default"
    }
}
