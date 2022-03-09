package org.cqfn.save.agent

import kotlinx.serialization.Serializable

/**
 * @property testSuiteName
 * @property countTest
 * @property countPassedTest
 */
@Serializable
data class LatestExecutionStatisticDto(
    val testSuiteName: String,
    val countTest: Int,
    val countPassedTest: Int,
)
