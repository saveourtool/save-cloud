package com.saveourtool.save.osv.processor

import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.warn
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Holder for all [OsvProcessor]
 */
@Component
class OsvProcessorHolder(
    private val osvProcessors: List<OsvProcessor.Custom>,
    private val defaultOsvProcessor: DefaultOsvProcessor,
) {
    /**
     * @param jsonObject
     * @return [VulnerabilityDto] processed by [OsvProcessor] resolved by ID or [DefaultOsvProcessor]
     */
    fun apply(jsonObject: JsonObject): Mono<VulnerabilityDto> {
        val id = jsonObject.getValue("id").jsonPrimitive.content
        val osvProcessor = osvProcessors
            .filter { it.supports(id) }
            .let { supportedOsvProcessors ->
                when (supportedOsvProcessors.size) {
                    0 -> defaultOsvProcessor
                    1 -> supportedOsvProcessors.first()
                    else -> {
                        log.warn {
                            "Fallback to default osv processor because found several processors for $id: ${supportedOsvProcessors.map { it.javaClass.simpleName }}"
                        }
                        defaultOsvProcessor
                    }
                }
            }
        return osvProcessor.apply(jsonObject)
    }

    companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val log = getLogger<OsvProcessorHolder>()
    }
}