package com.saveourtool.save.filters

import com.saveourtool.save.entities.OrganizationStatus
import com.saveourtool.save.utils.DATABASE_DELIMITER
import kotlinx.serialization.Serializable

/**
 * Filters for test suites
 * @status
 * @organizationName
 */
@Serializable
data class OrganizationAdminFilters(
    var status: List<OrganizationStatus>?,
    val organizationName: String
) {

    fun contains(elem: OrganizationStatus) = status?.contains(elem) ?: false

    fun changeStatus(elem: OrganizationStatus) {
        status = printChangeStatus(elem)
    }

    fun printChangeStatus(elem: String) =
        OrganizationStatus.values().firstOrNull { it.name == elem.uppercase() } ?. let { printChangeStatus(it) } ?: status

    private fun printChangeStatus(elem: OrganizationStatus) =
        if (this.contains(elem)) {
            status?.plus(elem)
        } else {
            status?.minus(elem)
        }

    companion object{
        val empty = OrganizationAdminFilters(emptyList(), "")
        val any = OrganizationAdminFilters(listOf(OrganizationStatus.CREATED, OrganizationStatus.DELETED, OrganizationStatus.BANNED), "")
    }
}
