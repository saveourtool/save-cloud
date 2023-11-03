/**
 * If you would like to add filtering to tables and have proper routing on react level to filtered views
 * than you can add extension methods to URLSearchParams and pass filter to particular view
 */

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.entities.vulnerability.VulnerabilityStatus
import com.saveourtool.save.filters.VulnerabilityFilter
import org.w3c.dom.url.URLSearchParams

/**
 * @return VulnerabilityFilter that can be passed to a table
 */
fun URLSearchParams.toVulnerabilitiesFilter(): VulnerabilityFilter {
    val tags = this.get("tags")?.split(",")?.toSet() ?: emptySet()
    val prefix = this.get("prefix") ?: ""
    val status = VulnerabilityStatus.values().find { it.name == this.get("status")?.uppercase() }
    val statuses = status?.let { listOf(it) } ?: listOf(VulnerabilityStatus.APPROVED, VulnerabilityStatus.AUTO_APPROVED)

    return VulnerabilityFilter(
        identifierPrefix = prefix,
        statuses = statuses,
        tags = tags
    )
}
