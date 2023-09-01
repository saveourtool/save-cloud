/**
 * This file contains util methods for CosvSchema
 */

package com.saveourtool.save.utils

import com.saveourtool.osv4k.TimeLineEntry
import com.saveourtool.osv4k.TimeLineEntryType
import com.saveourtool.save.entities.vulnerability.VulnerabilityDateDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityDateType
import com.saveourtool.save.entities.vulnerability.VulnerabilityLanguage
import com.saveourtool.save.info.UserInfo
import kotlinx.datetime.LocalDateTime
import com.saveourtool.osv4k.OsvSchema as CosvSchema

private const val saveourtoolProfilePrefix = "https://saveourtool.com/profile/"

/**
 * @return Save's contributors
 */
fun CosvSchema<*, *, *, *>.getSaveContributes(): List<UserInfo> = credits
    ?.flatMap { credit -> credit.contact.orEmpty() }
    ?.filter { it.startsWith(saveourtoolProfilePrefix) }
    ?.map { it.removePrefix(saveourtoolProfilePrefix) }
    ?.map { UserInfo(it) }
    .orEmpty()

/**
 * @return timeline as [List] of [VulnerabilityDateDto]
 */
fun CosvSchema<*, *, *, *>.getTimeline(): List<VulnerabilityDateDto> = buildList {
    timeLine?.map { it.asVulnerabilityDateDto(id) }?.let { addAll(it) }
    add(modified.asVulnerabilityDateDto(id, VulnerabilityDateType.MODIFIED))  // TODO: do we need it?
    published?.asVulnerabilityDateDto(id, VulnerabilityDateType.PUBLISHED)?.run { add(this) }
    withdrawn?.asVulnerabilityDateDto(id, VulnerabilityDateType.WITHDRAWN)?.run { add(this) }
}

private fun LocalDateTime.asVulnerabilityDateDto(cosvId: String, type: VulnerabilityDateType) = VulnerabilityDateDto(
    date = this,
    type = type,
    vulnerabilityName = cosvId,
)

private fun TimeLineEntry.asVulnerabilityDateDto(cosvId: String) = value.asVulnerabilityDateDto(cosvId,
    when (type) {
        TimeLineEntryType.introduced -> VulnerabilityDateType.INTRODUCED
        TimeLineEntryType.found -> VulnerabilityDateType.FOUND
        TimeLineEntryType.fixed -> VulnerabilityDateType.FIXED
        TimeLineEntryType.disclosed -> VulnerabilityDateType.DISCLOSED
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
