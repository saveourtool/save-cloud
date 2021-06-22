package org.cqfn.save.execution

import kotlinx.serialization.Serializable

/**
 * @property version
 * @property status
 * @property type
 * @property passedTests
 * @property failedTests
 * @property skippedTests
 * @property endTime
 */
@Serializable
@Suppress("LongParameterList")
class ExecutionDto(
    val status: ExecutionStatus,
    val type: ExecutionType,
    val version: String,
    val endTime: Long?,
    val passedTests: Long,
    val failedTests: Long,
    val skippedTests: Long,
)
