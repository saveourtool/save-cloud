package org.cqfn.save.execution

import kotlinx.serialization.Serializable

/**
 * @property id
 * @property version
 * @property status
 * @property type
 * @property passedTests
 * @property failedTests
 * @property skippedTests
 * @property endTime
 * @property additionalFiles
 */
@Serializable
@Suppress("LongParameterList")
data class ExecutionDto(
    val id: Long,
    val status: ExecutionStatus,
    val type: ExecutionType,
    val version: String?,
    val endTime: Long?,
    val passedTests: Long,
    val failedTests: Long,
    val skippedTests: Long,
    val additionalFiles: String?,
)
