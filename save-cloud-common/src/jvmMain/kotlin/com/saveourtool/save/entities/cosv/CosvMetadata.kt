package com.saveourtool.save.entities.cosv

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.spring.entity.BaseEntityWithDateAndDto
import kotlinx.datetime.toKotlinLocalDateTime
import javax.persistence.Entity

/**
 * @property cosvId [com.saveourtool.osv4k.OsvSchema.id]
 * @property summary [com.saveourtool.osv4k.OsvSchema.summary]
 * @property details [com.saveourtool.osv4k.OsvSchema.details]
 * @property severity [com.saveourtool.osv4k.Severity.score]
 * @property severityNum [com.saveourtool.osv4k.Severity.scoreNum]
 * @property user [User] who uploaded COSV to save
 * @property organization [Organization] to which COSV was uploaded
 **/
@Entity
class CosvMetadata(
    var cosvId: String,
    var summary: String,
    var details: String,
    var severity: String?,
    var severityNum: Int,
    var user: User,
    var organization: Organization,
) : BaseEntityWithDateAndDto<CosvMetadataDto>() {
    override fun toDto(): CosvMetadataDto = CosvMetadataDto(
        cosvId = cosvId,
        summary = summary,
        details = details,
        severity = severity,
        severityNum = severityNum,
        userId = user.requiredId(),
        organizationId = organization.requiredId(),
        updateDate = updateDate?.toKotlinLocalDateTime()
            ?: throw IllegalStateException("updateDate is not set on ${CosvMetadata::class.simpleName}"),
        createDate = createDate?.toKotlinLocalDateTime()
            ?: throw IllegalStateException("createDate is not set on ${CosvMetadata::class.simpleName}"),
    )

    companion object {
        /**
         * @receiver [CosvMetadataDto] dto to create [CosvMetadata]
         * @param userResolver
         * @param organizationResolver
         * @return [CosvMetadata] created from receiver
         */
        fun CosvMetadataDto.toEntity(
            userResolver: (Long) -> User,
            organizationResolver: (Long) -> Organization,
        ): CosvMetadata = CosvMetadata(
            cosvId = cosvId,
            summary = summary,
            details = details,
            severity = severity,
            severityNum = severityNum,
            user = userResolver(userId),
            organization = organizationResolver(organizationId),
        )
    }
}
