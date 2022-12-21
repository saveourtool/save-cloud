package com.saveourtool.save.test.analysis.internal

import com.saveourtool.save.test.analysis.api.TestId
import com.saveourtool.save.test.analysis.api.TestRun
import com.saveourtool.save.test.analysis.api.TestStatisticsStorage

/**
 * A [TestStatisticsStorage] which allows the statistical data stored to be
 * mutated.
 */
interface MutableTestStatisticsStorage : TestStatisticsStorage {
    /**
     * Updates stored statistical data for the test specified by [id] with a new
     * [testRun].
     *
     * @param id the unique id of the test.
     * @param testRun the recent test run information with a status and optional
     *   duration.
     */
    fun updateExecutionStatistics(id: TestId, testRun: TestRun)

    /**
     * Clears any statistical data collected.
     */
    fun clear()
}
