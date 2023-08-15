package com.saveourtool.save.osv.processor

import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

import kotlinx.serialization.json.JsonObject

/**
 * Holder for all [OsvProcessor]
 *
 * @param osvProcessors all [OsvProcessor] injected by Spring
 */
@Component
class OsvProcessorHolder(
    osvProcessors: List<OsvProcessor>,
) {
    private val osvProcessors = osvProcessors.associateBy { it.id }
    private val defaultOsvProcessor = this.osvProcessors[DefaultOsvProcessor.ID]
        ?: throw IllegalStateException("Not found default OsvProcessor")

    /**
     * @param jsonObject
     * @param id
     * @return [VulnerabilityDto] processed by [OsvProcessor] resolved by ID or [DefaultOsvProcessor]
     */
    fun process(
        id: String = DefaultOsvProcessor.ID,
        jsonObject: JsonObject,
    ): Mono<VulnerabilityDto> {
        val osvProcessor = osvProcessors[id] ?: getDefaultOsvProcessorAsFallback(id)
        return osvProcessor(jsonObject)
    }

    private fun getDefaultOsvProcessorAsFallback(id: String): OsvProcessor {
        log.debug {
            "Fallback to default osv processor because not found processor for $id: ${osvProcessors.map { it.javaClass.simpleName }}"
        }
        return defaultOsvProcessor
    }

    companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val log = getLogger<OsvProcessorHolder>()
    }
}
