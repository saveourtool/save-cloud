package com.saveourtool.save.entities.cosv

import com.saveourtool.save.entities.vulnerability.VulnerabilityDateDto
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.getRelatedLink
import com.saveourtool.osv4k.RawOsvSchema as RawCosvSchema

/**
 * @property metadata
 * @property cosv
 * @property saveContributors save's user from [com.saveourtool.osv4k.OsvSchema.contributors]
 * @property tags
 * @property timeline
 **/
data class RawCosvExt(
    val metadata: VulnerabilityMetadataDto,
    val cosv: RawCosvSchema,
    val saveContributors: List<UserInfo>,
    val tags: Set<String>,
    val timeline: List<VulnerabilityDateDto>,
) {
    /**
     * @return a vulnerability dto with description
     */
    fun toVulnerabilityDtoWithDescription() = metadata.toVulnerabilityDto().copy(
        description = metadata.details,
        relatedLink = cosv.getRelatedLink(),
    )
}
