/**
 * data classes representing additional data associated with test executions
 */

package org.cqfn.save.domain

import kotlinx.serialization.Serializable

/**
 * @property testSuiteName
 * @property pluginName
 * @property testLocation
 * @property testName
 */
@Serializable
data class TestResultLocation(
    val testSuiteName: String,
    val pluginName: String,
    val testLocation: String,
    val testName: String,
)

/**
 * @property testResultLocation
 * @property stdout
 * @property stderr
 * @property durationMillis
 */
@Serializable
data class TestResultDebugInfo(
    val testResultLocation: TestResultLocation,
    val stdout: String?,
    val stderr: String?,
    val durationMillis: Long?,
)
