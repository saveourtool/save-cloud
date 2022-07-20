package com.saveourtool.save.execution

import com.saveourtool.save.domain.TestResultStatus

/**
 * Aff filters in one property
 * @property status to filter by [status]
 * @property fileName to use filter by [fileName]
 * @property testSuite to use filter by [testSuite]
 * @property tag to use filter by [tag]
 */
data class TestExecutionFilters(
    var status: TestResultStatus?,

    var fileName: String?,

    var testSuite: String?,

    var tag: String?,
) {
    companion object {
        val EMPTY = TestExecutionFilters(status = null, fileName = "", testSuite = "", tag = "")
    }
}
