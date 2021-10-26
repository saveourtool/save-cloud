/**
 * data classes representing additional data associated with test executions
 */

package org.cqfn.save.domain

import org.cqfn.save.core.result.DebugInfo
import org.cqfn.save.core.result.TestStatus

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
 * @property debugInfo
 * @property testStatus
 */
@Serializable
data class TestResultDebugInfo(
    val testResultLocation: TestResultLocation,
    val debugInfo: DebugInfo?,
    val testStatus: TestStatus,
)
