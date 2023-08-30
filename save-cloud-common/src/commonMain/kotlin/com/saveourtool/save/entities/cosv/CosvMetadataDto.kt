package com.saveourtool.save.entities.cosv

import kotlinx.datetime.LocalDateTime

/**
 * @property cosvId [com.saveourtool.osv4k.OsvSchema.id]
 * @property summary [com.saveourtool.osv4k.OsvSchema.summary]
 * @property details [com.saveourtool.osv4k.OsvSchema.details]
 * @property severity [com.saveourtool.osv4k.Severity.score]
 * @property severityNum [com.saveourtool.osv4k.Severity.scoreNum]
 * @property userId [com.saveourtool.save.entities.User.id]
 * @property organizationId [com.saveourtool.save.entities.Organization.id]
 * @property updateDate [com.saveourtool.save.spring.entity.BaseEntityWithDate.updateDate]
 * @property createDate [com.saveourtool.save.spring.entity.BaseEntityWithDate.createDate]
 **/
data class CosvMetadataDto(
    val cosvId: String,
    val summary: String,
    val details: String,
    val severity: String?,
    val severityNum: Int,
    val userId: Long,
    val organizationId: Long,
    val updateDate: LocalDateTime,
    val createDate: LocalDateTime,
)
