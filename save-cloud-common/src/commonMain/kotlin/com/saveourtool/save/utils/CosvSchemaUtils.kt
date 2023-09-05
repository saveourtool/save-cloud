/**
 * This file contains util methods for CosvSchema
 */

package com.saveourtool.save.utils

import com.saveourtool.save.entities.vulnerability.VulnerabilityDateDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityDateType
import com.saveourtool.save.entities.vulnerability.VulnerabilityLanguage
import com.saveourtool.save.info.UserInfo

import com.saveourtool.osv4k.Credit
import com.saveourtool.osv4k.CreditType
import com.saveourtool.osv4k.OsvSchema as CosvSchema
import com.saveourtool.osv4k.ReferenceType
import com.saveourtool.osv4k.TimeLineEntry
import com.saveourtool.osv4k.TimeLineEntryType

import kotlinx.datetime.LocalDateTime

private const val SAVEOURTOOL_PROFILE_PREFIX = "https://saveourtool.com/profile/"

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
    timeLine?.map { it.asVulnerabilityDateDto(id) }?.let { addAll(it) }
    add(modified.asVulnerabilityDateDto(id, VulnerabilityDateType.MODIFIED))  // TODO: do we need it?
    published?.asVulnerabilityDateDto(id, VulnerabilityDateType.PUBLISHED)?.run { add(this) }
    withdrawn?.asVulnerabilityDateDto(id, VulnerabilityDateType.WITHDRAWN)?.run { add(this) }
}

/**
 * @return [TimeLineEntry]
 */
fun VulnerabilityDateDto.asTimelineEntry(): TimeLineEntry = TimeLineEntry(
    value = date,
    type = when (type) {
        VulnerabilityDateType.DISCLOSED -> TimeLineEntryType.disclosed
        VulnerabilityDateType.FIXED -> TimeLineEntryType.fixed
        VulnerabilityDateType.FOUND -> TimeLineEntryType.found
        VulnerabilityDateType.INTRODUCED -> TimeLineEntryType.introduced
        VulnerabilityDateType.MODIFIED -> throw IllegalArgumentException("Not supported date change")
        VulnerabilityDateType.PUBLISHED -> throw IllegalArgumentException("Not supported date change")
        VulnerabilityDateType.WITHDRAWN -> throw IllegalArgumentException("Not supported date change")
    }
)

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

private fun LocalDateTime.asVulnerabilityDateDto(cosvId: String, type: VulnerabilityDateType) = VulnerabilityDateDto(
    date = this,
    type = type,
    vulnerabilityIdentifier = cosvId,
)

private fun TimeLineEntry.asVulnerabilityDateDto(cosvId: String) = value.asVulnerabilityDateDto(cosvId,
    when (type) {
        TimeLineEntryType.introduced -> VulnerabilityDateType.INTRODUCED
        TimeLineEntryType.found -> VulnerabilityDateType.FOUND
        TimeLineEntryType.fixed -> VulnerabilityDateType.FIXED
        TimeLineEntryType.disclosed -> VulnerabilityDateType.DISCLOSED
    }
)
