/**
 * This file contains util methods for CosvSchema
 */

package com.saveourtool.save.utils

import com.saveourtool.save.entities.vulnerability.VulnerabilityDateDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityDateType
import com.saveourtool.save.entities.vulnerability.VulnerabilityLanguage
import com.saveourtool.save.info.UserInfo

import com.saveourtool.osv4k.*
import com.saveourtool.osv4k.OsvSchema as CosvSchema

import kotlinx.datetime.LocalDateTime

private const val SAVEOURTOOL_PROFILE_PREFIX = "https://saveourtool.com/profile/"

private val timelineEntryTypeMapping = mapOf(
    VulnerabilityDateType.DISCLOSED to TimelineEntryType.disclosed,
    VulnerabilityDateType.FIXED to TimelineEntryType.fixed,
    VulnerabilityDateType.FOUND to TimelineEntryType.found,
    VulnerabilityDateType.INTRODUCED to TimelineEntryType.introduced,
)

/**
 * @return Save's contributors
 */
fun CosvSchema<*, *, *, *>.getSaveContributes(): List<UserInfo> = credits
    ?.flatMap { credit -> credit.contact.orEmpty() }
    ?.filter { it.startsWith(SAVEOURTOOL_PROFILE_PREFIX) }
    ?.map { it.removePrefix(SAVEOURTOOL_PROFILE_PREFIX) }
    ?.map { UserInfo(it) }
    .orEmpty()

/**
 * @return [Credit]
 */
fun UserInfo.asCredit(): Credit = let {
    Credit(
        name = it.name,
        contact = listOf(
            SAVEOURTOOL_PROFILE_PREFIX + it.name
        ),
        type = CreditType.REPORTER,
    )
}

/**
 * @return list of [Credit]
 */
fun List<UserInfo>.asCredits(): List<Credit> = map { it.asCredit() }

/**
 * @return timeline as [List] of [VulnerabilityDateDto]
 */
fun CosvSchema<*, *, *, *>.getTimeline(): List<VulnerabilityDateDto> = buildList {
    timeline?.map { it.asVulnerabilityDateDto(id) }?.let { addAll(it) }
    add(modified.asVulnerabilityDateDto(id, VulnerabilityDateType.MODIFIED))  // TODO: do we need it?
    published?.asVulnerabilityDateDto(id, VulnerabilityDateType.PUBLISHED)?.run { add(this) }
    withdrawn?.asVulnerabilityDateDto(id, VulnerabilityDateType.WITHDRAWN)?.run { add(this) }
}

/**
 * @return [TimelineEntry]
 */
fun VulnerabilityDateDto.asTimelineEntry(): TimelineEntry = timelineEntryTypeMapping[type]
    ?.let {
        TimelineEntry(
            value = date,
            type = it,
        )
    }
    ?: throw IllegalArgumentException("VulnerabilityDate $type cannot be saved in COSV timeline")

/**
 * @return language as [VulnerabilityLanguage]
 */
fun CosvSchema<*, *, *, *>.getLanguage(): VulnerabilityLanguage? = affected?.firstNotNullOfOrNull { affected ->
    affected.`package`?.language?.let { language ->
        VulnerabilityLanguage.values().firstOrNull { it.value == language }
    }
}

/**
 * @return relatedLink
 */
fun CosvSchema<*, *, *, *>.getRelatedLink(): String? = references
    ?.filter { it.type == ReferenceType.WEB }?.map { it.url }?.firstOrNull()

/**
 * @return Severity for a single progress
 */
fun Int.asSeverity(): Severity = Severity(
    type = SeverityType.CVSS_V3,
    score = "N/A",
    scoreNum = toString(),
)

private fun LocalDateTime.asVulnerabilityDateDto(cosvId: String, type: VulnerabilityDateType) = VulnerabilityDateDto(
    date = this,
    type = type,
    vulnerabilityIdentifier = cosvId,
)

private fun TimelineEntry.asVulnerabilityDateDto(cosvId: String) = value.asVulnerabilityDateDto(cosvId,
    when (type) {
        TimelineEntryType.introduced -> VulnerabilityDateType.INTRODUCED
        TimelineEntryType.found -> VulnerabilityDateType.FOUND
        TimelineEntryType.fixed -> VulnerabilityDateType.FIXED
        TimelineEntryType.disclosed -> VulnerabilityDateType.DISCLOSED
    }
)
