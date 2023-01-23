package com.saveourtool.save.test.analysis.internal

import com.saveourtool.save.test.analysis.api.TestStatisticsStorage

/**
 * A [TestStatisticsStorage] which allows the statistical data stored to be
 * mutated.
 */
interface MutableTestStatisticsStorage : TestStatisticsStorage {
    /**
     * Updates stored statistical data with a new [testRunExt].
     *
     * @param testRunExt the recent test run information with a status and
     *   optional duration.
     */
    fun updateExecutionStatistics(testRunExt: ExtendedTestRun)

    /**
     * Clears any statistical data collected.
     */
    fun clear()
}
