package com.saveourtool.save.execution

import kotlinx.serialization.Serializable

/**
 * @property id
 * @property version
 * @property status
 * @property type
 * @property runningTests
 * @property passedTests
 * @property failedTests
 * @property skippedTests
 * @property endTime
 * @property additionalFiles
 * @property startTime
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
    val runningTests: Long,
    val passedTests: Long,
    val failedTests: Long,
    val skippedTests: Long,
    val additionalFiles: List<String>?,
)
