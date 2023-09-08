package com.saveourtool.save.entities.cosv

import com.saveourtool.save.entities.OrganizationDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityLanguage
import com.saveourtool.save.entities.vulnerability.VulnerabilityStatus
import com.saveourtool.save.info.UserInfo
import kotlinx.datetime.LocalDateTime

/**
 * @property cosvId [com.saveourtool.osv4k.OsvSchema.id]
 * @property summary [com.saveourtool.osv4k.OsvSchema.summary]
 * @property details [com.saveourtool.osv4k.OsvSchema.details]
 * @property severityNum [com.saveourtool.osv4k.Severity.scoreNum]
 * @property modified [com.saveourtool.osv4k.OsvSchema.modified]
 * @property submitted
 * @property user
 * @property organization
 * @property language
 * @property status
 **/
data class CosvMetadataDto(
    val cosvId: String,
    val summary: String,
    val details: String,
    val severityNum: Int,
    val modified: LocalDateTime,
    val submitted: LocalDateTime,
    val user: UserInfo,
    val organization: OrganizationDto?,
    val language: VulnerabilityLanguage,
    val status: VulnerabilityStatus,
) {
    /**
     * @return a vulnerability dto
     */
    fun toVulnerabilityDto() = VulnerabilityDto(
        identifier = cosvId,
        progress = severityNum,
        projects = emptyList(),  // TODO: not supported yet
        description = null,  // it's empty by old logic
        shortDescription = summary,
        relatedLink = null,  // FIXME: related link is not available here
        language = language,
        userInfo = user,  // it was empty in old logic, but will populate to simplify the logic
        organization = organization,
        dates = emptyList(),
        participants = emptyList(),
        status = status,
        creationDateTime = submitted,
        lastUpdatedDateTime = modified,
    )
}
