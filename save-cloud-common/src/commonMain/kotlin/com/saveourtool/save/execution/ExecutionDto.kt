package com.saveourtool.save.execution

import kotlinx.serialization.Serializable

/**
 * @property id
 * @property status
 * @property type
 * @property version
 * @property startTime
 * @property endTime
 * @property allTests
 * @property runningTests
 * @property passedTests
 * @property failedTests
 * @property skippedTests
 * @property unmatchedChecks
 * @property matchedChecks
 * @property expectedChecks
 * @property unexpectedChecks
 */
@Serializable
@Suppress("LongParameterList")
data class ExecutionDto(
    val id: Long,
    val status: ExecutionStatus,
    val type: ExecutionType,
    val version: String?,
    val startTime: Long,
    val endTime: Long?,
    val allTests: Long,
    val runningTests: Long,
    val passedTests: Long,
    val failedTests: Long,
    val skippedTests: Long,
    val unmatchedChecks: Long,
    val matchedChecks: Long,
    val expectedChecks: Long,
    val unexpectedChecks: Long,
)
