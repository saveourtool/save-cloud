package com.saveourtool.save.entities.cosv

import com.saveourtool.save.entities.vulnerability.VulnerabilityDateDto
import com.saveourtool.save.info.UserInfo
import com.saveourtool.osv4k.RawOsvSchema as RawCosvSchema

/**
 * @property metadata
 * @property rawContent
 * @property saveContributors save's user from [com.saveourtool.osv4k.OsvSchema.contributors]
 * @property tags
 * @property timeline
 **/
data class RawCosvExt(
    val metadata: VulnerabilityMetadataDto,
    val rawContent: RawCosvSchema,
    val saveContributors: List<UserInfo>,
    val tags: Set<String>,
    val timeline: List<VulnerabilityDateDto>,
)
