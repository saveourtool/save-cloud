package com.saveourtool.save.filters

import com.saveourtool.save.entities.OrganizationStatus
import kotlinx.serialization.Serializable

/**
 * @property prefix substring that match the beginning of a name
 * @property status current status of an organization
 */
@Serializable
data class OrganizationFilters(
    val prefix: String,
    val status: List<OrganizationStatus> = listOf(OrganizationStatus.CREATED),
) {
    companion object {
        val empty = OrganizationFilters("")
        val all = OrganizationFilters("", OrganizationStatus.values().toList())
    }
}
