package com.saveourtool.save.entities.cosv

import com.saveourtool.save.spring.entity.BaseEntityWithDateAndDto
import javax.persistence.Entity

/**
 * @property cosvId [com.saveourtool.osv4k.OsvSchema.id]
 * @property summary [com.saveourtool.osv4k.OsvSchema.summary]
 * @property details [com.saveourtool.osv4k.OsvSchema.details]
 * @property severity [com.saveourtool.osv4k.Severity.score]
 * @property severityNum [com.saveourtool.osv4k.Severity.scoreNum]
 **/
@Entity
class CosvMetadata(
    var cosvId: String,
    var summary: String,
    var details: String,
    var severity: String?,
    var severityNum: Int,
): BaseEntityWithDateAndDto<CosvMetadataDto>() {
    override fun toDto(): CosvMetadataDto = CosvMetadataDto(
        cosvId = cosvId,
        summary = summary,
        details = details,
        severity = severity,
        severityNum = severityNum,
    )
}
