package com.saveourtool.save.test.analysis.internal

import com.saveourtool.save.test.analysis.api.TestRun
import com.saveourtool.save.test.analysis.api.TestRuns

/**
 * Extended information about the run sequence of a specific test (enriched with
 * [lastExecutionId]).
 *
 * @property testRuns the list of [TestRun] instances.
 * @property lastExecutionId the last execution id seen for this particular test.
 */
internal data class ExtendedTestRuns(
    val testRuns: TestRuns,
    val lastExecutionId: Long,
) : TestRuns by testRuns
