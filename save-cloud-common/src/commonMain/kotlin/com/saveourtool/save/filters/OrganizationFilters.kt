package com.saveourtool.save.filters

import kotlinx.serialization.Serializable

/**
 * @property name
 */
@Serializable
data class OrganizationFilters(
    val name: String?,
)
