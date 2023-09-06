package com.saveourtool.save.entities.cosv

import com.saveourtool.save.entities.vulnerability.VulnerabilityDateDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityProjectDto
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.getRelatedLink
import com.saveourtool.save.utils.getTimeline

import com.saveourtool.osv4k.RawOsvSchema as RawCosvSchema

/**
 * @property metadata
 * @property cosv
 * @property saveContributors save's user from [com.saveourtool.osv4k.OsvSchema.contributors]
 * @property tags
 * @property timeline
 **/
data class RawCosvExt(
    val metadata: CosvMetadataDto,
    val cosv: RawCosvSchema,
    val saveContributors: List<UserInfo>,
    val tags: Set<String>,
    val timeline: List<VulnerabilityDateDto>,
) {
    /**
     * TODO: Copied from VulnerabilityDto
     */
    val identifier = metadata.cosvId

    /**
     * TODO: Copied from VulnerabilityDto
     */
    val progress = metadata.severityNum

    /**
     * TODO: A stub for [projects]
     */
    val projects: List<VulnerabilityProjectDto> = emptyList()

    /**
     * TODO: Copied from VulnerabilityDto
     */
    val shortDescription = metadata.summary

    /**
     * TODO: Copied from VulnerabilityDto
     */
    val relatedLink = cosv.getRelatedLink()

    /**
     * TODO: Copied from VulnerabilityDto
     */
    val language = metadata.language

    /**
     * TODO: Copied from VulnerabilityDto
     */
    val userInfo = metadata.user

    /**
     * TODO: Copied from VulnerabilityDto
     */
    val organization = metadata.organization

    /**
     * TODO: Copied from VulnerabilityDto
     */
    val dates = cosv.getTimeline()

    /**
     * TODO: Copied from VulnerabilityDto
     */
    val participants = saveContributors

    /**
     * TODO: Copied from VulnerabilityDto
     */
    val creationDateTime = metadata.published

    /**
     * TODO: Copied from VulnerabilityDto
     */
    val lastUpdatedDateTime = metadata.modified

    /**
     * TODO: Copied from VulnerabilityDto
     */
    val status = metadata.status

    /**
     * @return map where key is LocalDateTime and value is a label of LocalDateTime
     */
    fun getDatesWithLabels() = timeline.associate { it.type.value to it.date }

    /**
     * @return all [saveContributors] and owner's [metadata.user]
     */
    fun getAllParticipants(): List<UserInfo> = listOf(metadata.user).plus(saveContributors)

    /**
     * @return a vulnerability dto with description
     */
    fun toVulnerabilityDtoWithDescription() = metadata.toVulnerabilityDto().copy(
        description = metadata.details,
        relatedLink = cosv.getRelatedLink(),
    )
}
