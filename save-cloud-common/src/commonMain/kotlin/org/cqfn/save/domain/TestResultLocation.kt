package org.cqfn.save.domain

import kotlinx.serialization.Serializable

@Serializable
data class TestResultLocation(
    val testSuiteName: String,
    val pluginName: String,
    val testLocation: String,
    val testName: String,
)

@Serializable
data class TestResultDebugInfo(
    val testResultLocation: TestResultLocation,
    val stdout: String?,
    val stderr: String?,
    val durationMillis: Long?,
)
