package com.saveourtool.save.execution

import com.saveourtool.save.domain.TestResultStatus
import kotlinx.serialization.Serializable

/**
 * Aff filters in one property
 * @property status to filter by [status]
 * @property fileName to use filter by [fileName]
 * @property testSuite to use filter by [testSuite]
 * @property tag to use filter by [tag]
 */
@Serializable
data class TestExecutionFilters(
    val status: TestResultStatus?,

    val fileName: String?,

    val testSuite: String?,

    val tag: String?,
) {
    companion object {
        val empty = TestExecutionFilters(status = null, fileName = null, testSuite = null, tag = null)
    }
}
