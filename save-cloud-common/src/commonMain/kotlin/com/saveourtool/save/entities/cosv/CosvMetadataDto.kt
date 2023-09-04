package com.saveourtool.save.entities.cosv

import com.saveourtool.save.entities.OrganizationDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityLanguage
import com.saveourtool.save.entities.vulnerability.VulnerabilityStatus
import com.saveourtool.save.info.UserInfo
import kotlinx.datetime.LocalDateTime

/**
 * @property cosvId [com.saveourtool.osv4k.OsvSchema.id]
 * @property summary [com.saveourtool.osv4k.OsvSchema.summary]
 * @property details [com.saveourtool.osv4k.OsvSchema.details]
 * @property severity [com.saveourtool.osv4k.Severity.score]
 * @property severityNum [com.saveourtool.osv4k.Severity.scoreNum]
 * @property modified [com.saveourtool.osv4k.OsvSchema.modified]
 * @property published [com.saveourtool.osv4k.OsvSchema.published]
 * @property user
 * @property organization
 * @property language
 * @property status
 **/
data class CosvMetadataDto(
    val cosvId: String,
    val summary: String,
    val details: String,
    val severity: String?,
    val severityNum: Int,
    val modified: LocalDateTime,
    val published: LocalDateTime,
    val user: UserInfo,
    val organization: OrganizationDto?,
    val language: VulnerabilityLanguage,
    val status: VulnerabilityStatus,
)
