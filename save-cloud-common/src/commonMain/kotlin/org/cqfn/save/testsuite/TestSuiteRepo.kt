package org.cqfn.save.testsuite

import kotlinx.serialization.Serializable

/**
 * @property gitUrl
 * @property propertiesRelativePaths
 */
@Serializable
data class TestSuiteRepo(
    val gitUrl: String,
    val propertiesRelativePaths: List<String>,
)
