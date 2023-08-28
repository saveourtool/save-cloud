package com.saveourtool.save.osv.processor

import com.saveourtool.save.entities.vulnerability.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.osv.storage.OsvStorage

import com.saveourtool.osv4k.OsvSchema
import com.saveourtool.osv4k.TimeLineEntry
import com.saveourtool.osv4k.TimeLineEntryType
import reactor.core.publisher.Mono

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Default implementation of [OsvProcessor] which uses only core fields
 */
@Suppress("GENERIC_NAME")
abstract class AbstractOsvProcessor<D : Any, A_E : Any, A_D : Any, A_R_D : Any>(
    private val osvStorage: OsvStorage,
    @Suppress("TYPE_ALIAS")
    private val serializer: KSerializer<OsvSchema<D, A_E, A_D, A_R_D>>
) : OsvProcessor {
    override fun invoke(jsonObject: JsonObject): Mono<VulnerabilityDto> {
        val osv = Json.decodeFromJsonElement(serializer, jsonObject)
        return osvStorage.upload(osv, serializer).map {
            createFromCoreFields(osv).updateBySpecificFields(osv)
        }
    }

    /**
     * @param osv
     * @return updated [VulnerabilityDto] by specific fields
     */
    protected abstract fun VulnerabilityDto.updateBySpecificFields(osv: OsvSchema<D, A_E, A_D, A_R_D>): VulnerabilityDto

    private fun <T : AnyOsvSchema> createFromCoreFields(osv: T): VulnerabilityDto = VulnerabilityDto(
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
        dates = buildList {
            osv.timeLine?.map { it.asVulnerabilityDateDto() }?.let { addAll(it) }
            add(osv.modified.asVulnerabilityDateDto(VulnerabilityDateType.CVE_UPDATED))  // TODO: do we need it?
            osv.published?.asVulnerabilityDateDto(VulnerabilityDateType.INTRODUCED)?.run { add(this) }
            osv.withdrawn?.asVulnerabilityDateDto(VulnerabilityDateType.FIXED)?.run { add(this) }
        },
        participants = emptyList(),
        status = VulnerabilityStatus.CREATED,
        tags = setOf("osv-schema")
    )

    companion object {
        private fun LocalDateTime.asVulnerabilityDateDto(type: VulnerabilityDateType) = VulnerabilityDateDto(
            date = this,
            type = type,
            vulnerabilityName = "NOT_USED_WHEN_SAVING_IN_DATABASE",
        )

        private fun TimeLineEntry.asVulnerabilityDateDto() = VulnerabilityDateDto(
            date = value,
            type = when (type) {
                TimeLineEntryType.introduced -> VulnerabilityDateType.INTRODUCED
                TimeLineEntryType.found -> VulnerabilityDateType.DISCOVERED
                TimeLineEntryType.fixed -> VulnerabilityDateType.FIXED
                TimeLineEntryType.disclosed -> VulnerabilityDateType.RELEASED
            },
            vulnerabilityName = "NOT_USED_WHEN_SAVING_IN_DATABASE",
        )
    }
}
