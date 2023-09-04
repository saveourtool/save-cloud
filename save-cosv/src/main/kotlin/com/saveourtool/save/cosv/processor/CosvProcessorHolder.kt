package com.saveourtool.save.cosv.processor

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.CosvMetadataDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

import kotlinx.serialization.json.JsonObject

/**
 * Holder for all [CosvProcessor]
 *
 * @param cosvProcessors all [CosvProcessor] injected by Spring
 */
@Component
class CosvProcessorHolder(
    cosvProcessors: List<CosvProcessor>,
) {
    private val cosvProcessors = cosvProcessors.associateBy { it.id }
    private val defaultCosvProcessor = this.cosvProcessors[DefaultCosvProcessor.ID]
        ?: throw IllegalStateException("Not found default OsvProcessor")

    /**
     * @param jsonObject
     * @param id
     * @param user
     * @param organization
     * @return [CosvMetadataDto] processed by [CosvProcessor] resolved by ID or [DefaultCosvProcessor]
     */
    fun process(
        id: String = DefaultCosvProcessor.ID,
        jsonObject: JsonObject,
        user: User,
        organization: Organization,
    ): Mono<CosvMetadataDto> {
        val cosvProcessor = cosvProcessors[id] ?: getDefaultOsvProcessorAsFallback(id)
        return cosvProcessor.process(jsonObject, user, organization)
    }

    private fun getDefaultOsvProcessorAsFallback(id: String): CosvProcessor {
        log.debug {
            "Fallback to default osv processor because not found processor for $id: ${cosvProcessors.map { it.javaClass.simpleName }}"
        }
        return defaultCosvProcessor
    }

    companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val log = getLogger<CosvProcessorHolder>()
    }
}
