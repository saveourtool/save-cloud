package com.saveourtool.save.cosv.processor

import com.saveourtool.save.cosv.repository.CosvRepository
import com.saveourtool.save.cosv.repository.CosvSchema
import com.saveourtool.save.cosv.repository.CosvSchemaKSerializer
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.vulnerability.*
import com.saveourtool.save.info.UserInfo

import com.saveourtool.osv4k.TimeLineEntry
import com.saveourtool.osv4k.TimeLineEntryType
import com.saveourtool.save.utils.getTimeline
import reactor.core.publisher.Mono

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Default implementation of [CosvProcessor] which uses only core fields
 */
@Suppress("GENERIC_NAME")
abstract class AbstractCosvProcessor<D : Any, A_E : Any, A_D : Any, A_R_D : Any>(
    private val cosvRepository: CosvRepository,
    private val serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>,
) : CosvProcessor {
    override fun process(
        jsonObject: JsonObject,
        user: User,
        organization: Organization,
    ): Mono<VulnerabilityDto> {
        val osv = Json.decodeFromJsonElement(serializer, jsonObject)
        return cosvRepository.save(osv, serializer, user, organization).map {
            createFromCoreFields(osv).updateBySpecificFields(osv)
        }
    }

    /**
     * @param osv
     * @return updated [VulnerabilityDto] by specific fields
     */
    protected abstract fun VulnerabilityDto.updateBySpecificFields(osv: CosvSchema<D, A_E, A_D, A_R_D>): VulnerabilityDto

    private fun <T : AnyCosvSchema> createFromCoreFields(osv: T): VulnerabilityDto = VulnerabilityDto(
        name = osv.id,
        vulnerabilityIdentifier = osv.id,  // should be replaced by alias
        progress = 0,  // TODO: it can be presented in two ways cvss v3 and cvss v2
        projects = emptyList(),  // TODO: need to refactor VulnerabilityProjectDto, COSV is basic
        description = osv.details,
        shortDescription = osv.summary.orEmpty(),
        relatedLink = null,
        language = VulnerabilityLanguage.OTHER,  // it seems to be removed, since language is invalid here and valid on package level (affected)
        userInfo = UserInfo(name = ""),  // will be set on saving to database
        organization = null,
        dates = osv.getTimeline(),
        participants = emptyList(),
        status = VulnerabilityStatus.CREATED,
        tags = setOf("cosv-schema")
    )
}
