package com.saveourtool.save.domain

/**
 * @property executionId
 * @property testResultLocation
 */
data class DebugInfoStorageKey(
    val executionId: Long,
    val testResultLocation: TestResultLocation,
)
