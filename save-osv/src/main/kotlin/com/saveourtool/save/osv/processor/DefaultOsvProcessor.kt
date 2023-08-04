package com.saveourtool.save.osv.processor

import com.saveourtool.save.entities.vulnerability.*
import com.saveourtool.save.info.UserInfo

import org.springframework.stereotype.Component

import kotlinx.datetime.LocalDateTime

/**
 * Default implementation of [OsvProcessor] which uses only core fields
 */
@Component
class DefaultOsvProcessor : OsvProcessor<AnyOsvSchema> {
    override fun <T : AnyOsvSchema> apply(osv: T): VulnerabilityDto = VulnerabilityDto(
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
