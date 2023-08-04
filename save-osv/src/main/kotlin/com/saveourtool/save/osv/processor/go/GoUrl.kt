package com.saveourtool.save.osv.processor.go

import kotlinx.serialization.Serializable

/**
 * Go Vulnerability Database `root.database_specific`
 *
 * @property url
 */
@Serializable
data class GoUrl(
    val url: String,
)
