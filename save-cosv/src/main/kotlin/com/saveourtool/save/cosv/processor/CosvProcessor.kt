package com.saveourtool.save.cosv.processor

import com.saveourtool.save.cosv.repository.CosvRepository
import com.saveourtool.save.cosv.repository.CosvSchemaKSerializer
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityLanguage
import com.saveourtool.save.entities.vulnerability.VulnerabilityStatus
import com.saveourtool.save.utils.getLanguage
import com.saveourtool.save.utils.getSaveContributes
import com.saveourtool.save.utils.getTimeline

import com.saveourtool.osv4k.RawOsvSchema
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer

/**
 * Processor of COSV entry which saves provided entry in saveourtool platform.
 */
@Component
class CosvProcessor(
    private val cosvRepository: CosvRepository,
) {
    private val rawSerializer: CosvSchemaKSerializer<JsonObject, JsonObject, JsonObject, JsonObject> = serializer()

    /**
     * @param jsonObject should contain a single object only
     * @param user who uploads
     * @param organization to which is uploaded
     * @return [VulnerabilityDto]
     */
    fun process(
        jsonObject: JsonObject,
        user: User,
        organization: Organization,
    ): Mono<VulnerabilityDto> {
        val cosv = Json.decodeFromJsonElement(rawSerializer, jsonObject)
        return cosvRepository.save(cosv, rawSerializer, user, organization).map {
            createFromCoreFields(cosv, user, organization)
        }
    }

    private fun createFromCoreFields(
        osv: RawOsvSchema,
        user: User,
        organization: Organization,
    ): VulnerabilityDto = VulnerabilityDto(
        identifier = osv.id,
        progress = osv.severity?.firstOrNull()?.scoreNum?.toInt() ?: 0,
        projects = emptyList(),  // TODO: need to refactor VulnerabilityProjectDto, COSV is basic
        description = osv.details,
        shortDescription = osv.summary.orEmpty(),
        relatedLink = null,
        language = osv.getLanguage() ?: VulnerabilityLanguage.OTHER,
        userInfo = user.toUserInfo(),
        organization = organization.toDto(),
        dates = osv.getTimeline(),
        participants = osv.getSaveContributes(),
        status = VulnerabilityStatus.CREATED,
        tags = setOf("cosv-schema")
    )
}
