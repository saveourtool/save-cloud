package com.saveourtool.save.entities.cosv

import com.saveourtool.osv4k.RawOsvSchema as RawCosvSchema

/**
 * @property metadata
 * @property rawContent
 * @property saveContributors save's user from [com.saveourtool.osv4k.OsvSchema.contributors]
 * @property tags
 **/
data class RawCosvExt(
    val metadata: CosvMetadataDto,
    val rawContent: RawCosvSchema,
    val saveContributors: List<Long>,
    val tags: Set<String>,
)
