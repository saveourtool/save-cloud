package com.saveourtool.save.test.analysis.algorithms

import com.saveourtool.save.test.analysis.api.TestRuns
import com.saveourtool.save.test.analysis.api.metrics.RegularTestMetrics
import com.saveourtool.save.test.analysis.api.results.FlakyTest
import com.saveourtool.save.test.analysis.api.results.IrregularTest

/**
 * _Flip-rate_ based flaky test detection algorithm.
 *
 * @param minimumRunCount the minimum run count a sample should have to be
 *   representative.
 * @param flipRateThreshold the _flip rate_ threshold.
 *   If the threshold is exceeded, the test is considered _flaky_.
 */
@Suppress("FLOAT_IN_ACCURATE_CALCULATIONS")
class FlipRateAnalysis(
    private val minimumRunCount: Int,
    private val flipRateThreshold: Double,
) : Algorithm {
    init {
        require(minimumRunCount > 0) {
            "Minimum run count should be positive: $minimumRunCount"
        }
        require(0.0 < flipRateThreshold && flipRateThreshold < 1.0) {
            "Flip Rate threshold should be in range (0.0, 1.0): $flipRateThreshold"
        }
    }

    override fun invoke(runs: TestRuns, metrics: RegularTestMetrics): IrregularTest? {
        require(runs.size == metrics.runCount) {
            "${runs.size} != ${metrics.runCount}"
        }

        return when {
            metrics.flipRate > flipRateThreshold && metrics.runCount >= minimumRunCount ->
                FlakyTest("Flip rate of ${metrics.flipRatePercentage}% over ${metrics.runCount} run(s)")

            else -> null
        }
    }
}
