package com.saveourtool.save.osv.processor

import com.saveourtool.save.entities.vulnerability.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.osv.storage.OsvStorage

import com.saveourtool.osv4k.OsvSchema
import reactor.core.publisher.Mono

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Default implementation of [OsvProcessor] which uses only core fields
 */
@Suppress("GENERIC_NAME")
abstract class AbstractOsvProcessor<D : Any, A_D : Any, A_E : Any, A_R_D : Any>(
    private val osvStorage: OsvStorage,
) : OsvProcessor {
    override fun invoke(jsonObject: JsonObject): Mono<VulnerabilityDto> {
        val osv: OsvSchema<D, A_D, A_E, A_R_D> = Json.decodeFromJsonElement(jsonObject)
        return osvStorage.upload(osv).map {
            createFromCoreFields(osv).updateBySpecificFields(osv)
        }
    }

    /**
     * @param osv
     * @return updated [VulnerabilityDto] by specific fields
     */
    protected abstract fun VulnerabilityDto.updateBySpecificFields(osv: OsvSchema<D, A_D, A_E, A_R_D>): VulnerabilityDto

    private fun <T : AnyOsvSchema> createFromCoreFields(osv: T): VulnerabilityDto = VulnerabilityDto(
        name = osv.id,
        vulnerabilityIdentifier = osv.id,
        progress = 0,
        projects = emptyList(),
        description = osv.details,
        shortDescription = osv.summary.orEmpty(),
        relatedLink = null,
        language = VulnerabilityLanguage.OTHER,
        userInfo = UserInfo(name = ""),  // will be set on saving to database
        organization = null,
        dates = buildList {
            add(osv.modified.asVulnerabilityDateDto(VulnerabilityDateType.CVE_UPDATED))
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
            vulnerabilityName = "NOT_USED_ON_SAVE",
        )
    }
}
