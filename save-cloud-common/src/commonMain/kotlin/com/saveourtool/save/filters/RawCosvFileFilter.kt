package com.saveourtool.save.filters

import kotlinx.serialization.Serializable

@Serializable
data class RawCosvFileFilter(
    val fileNamePart: String?,
    val organizationName: String,
) {
}