@file:JvmName("TestExecutionUtils")
@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.backend.utils

import com.saveourtool.save.entities.TestExecution
import com.saveourtool.save.test.analysis.api.TestRun
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlinx.datetime.Instant
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.UtcOffset.Companion.ZERO
import kotlinx.datetime.toInstant
import kotlinx.datetime.toKotlinLocalDateTime

/**
 * Represents this test execution with a smaller [TestRun] instance, allowing to
 * conserve memory.
 *
 * @return the [TestRun] view of this test execution.
 */
internal fun TestExecution.asTestRun(): TestRun =
        TestRun(status, durationOrNull())

/**
 * @return the duration of this test execution as `kotlin.time.Duration`.
 */
private fun TestExecution.durationOrNull(): Duration? {
    val offset = ZERO
    val startTime = startTime?.toInstant(offset) ?: return null
    val endTime = endTime?.toInstant(offset) ?: return null

    return endTime - startTime
}

private fun LocalDateTime.toInstant(offset: UtcOffset): Instant =
        toKotlinLocalDateTime().toInstant(offset)
