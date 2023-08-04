package com.saveourtool.save.osv.processor

import com.saveourtool.osv4k.RawOsvSchema
import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.osv.storage.OsvStorage
import kotlinx.serialization.json.JsonObject
import org.springframework.stereotype.Component

/**
 * Default implementation of [OsvProcessor] which uses only core fields
 */
@Component
class DefaultOsvProcessor(
    osvStorage: OsvStorage,
) : AbstractOsvProcessor<JsonObject, JsonObject, JsonObject, JsonObject>(osvStorage) {
    override fun VulnerabilityDto.updateBySpecificFields(osv: RawOsvSchema): VulnerabilityDto = this
}
