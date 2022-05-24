/**
 * data classes representing additional data associated with test executions
 */

package com.saveourtool.save.domain

import com.saveourtool.save.core.result.DebugInfo
import com.saveourtool.save.core.result.TestStatus

import kotlinx.serialization.Serializable

/**
 * Represents data necessary to locate a test
 *
 * @property testSuiteName name of test suite
 * @property pluginName name of the plugin, which executes this test
 * @property testLocation path to the directory with test
 * @property testName name of the test file
 */
@Serializable
data class TestResultLocation(
    val testSuiteName: String,
    val pluginName: String,
    val testLocation: String,
    val testName: String,
)

/**
 * Represents additional data for test execution
 *
 * @property testResultLocation location of the test
 * @property debugInfo [DebugInfo] from execution of the test
 * @property testStatus [TestStatus] from execution of the test
 */
@Serializable
data class TestResultDebugInfo(
    val testResultLocation: TestResultLocation,
    val debugInfo: DebugInfo?,
    val testStatus: TestStatus,
)
