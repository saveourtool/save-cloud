package com.saveourtool.save.filters

import com.saveourtool.save.entities.OrganizationStatus
import kotlinx.serialization.Serializable

/**
 * @property prefix substring that match the beginning of a name
 * @property statuses current [statuses] of an organization
 */
@Serializable
data class OrganizationFilters(
    val prefix: String,
    val statuses: Set<OrganizationStatus> = setOf(OrganizationStatus.CREATED),
) {
    companion object {
        val created = OrganizationFilters("")
        val all = OrganizationFilters("", OrganizationStatus.values().toSet())
    }
}
