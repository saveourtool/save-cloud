package com.saveourtool.save.filters

import kotlinx.serialization.Serializable

/**
 * @property fileNamePart
 * @property organizationName
 */
@Serializable
data class RawCosvFileFilter(
    val fileNamePart: String?,
    val organizationName: String,
)
