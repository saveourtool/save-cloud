package com.saveourtool.save.filters

import com.saveourtool.save.entities.vulnerability.VulnerabilityLanguage
import com.saveourtool.save.entities.vulnerability.VulnerabilityStatus
import kotlinx.serialization.Serializable

/**
 * @property prefixId
 * @property status
 * @property tags
 * @property language
 * @property authorName
 * @property organizationName
 */
@Serializable
data class CosvFilter(
    val prefixId: String,
    val status: VulnerabilityStatus?,
    val tags: Set<String> = emptySet(),
    val language: VulnerabilityLanguage? = null,
    val authorName: String? = null,
    val organizationName: String? = null,
) {
    companion object {
        val approved = CosvFilter("", status = VulnerabilityStatus.APPROVED)
    }
}
