package com.saveourtool.save.entities.cosv

/**
 * @property cosvId [com.saveourtool.osv4k.OsvSchema.id]
 * @property summary [com.saveourtool.osv4k.OsvSchema.summary]
 * @property details [com.saveourtool.osv4k.OsvSchema.details]
 * @property severity [com.saveourtool.osv4k.Severity.score]
 * @property severityNum [com.saveourtool.osv4k.Severity.scoreNum]
 **/
data class CosvMetadataDto(
    val cosvId: String,
    val summary: String,
    val details: String,
    val severity: String?,
    val severityNum: Int,
)
