package com.saveourtool.save.test.analysis.api.metrics

import kotlin.time.Duration

/**
 * @property successCount the number of successful test runs in a sample.
 * @property failureCount the number of failed test runs in a sample.
 * @property flipCount the number of test status "flips" in a sample.
 * @property ignoredCount the number of times the test was ignored (skipped).
 * @property averageDurationOrNull average test duration (if batch size is 1) or
 *   average batch duration (otherwise).
 * @property medianDurationOrNull median test duration (if batch size is 1) or
 *   median batch duration (otherwise).
 */
@Suppress(
    "MagicNumber",
    "FLOAT_IN_ACCURATE_CALCULATIONS",
    "MAGIC_NUMBER",
    "CUSTOM_GETTERS_SETTERS",
)
data class RegularTestMetrics(
    val successCount: Int,
    val failureCount: Int,
    val flipCount: Int,
    val ignoredCount: Int,
    val averageDurationOrNull: Duration?,
    val medianDurationOrNull: Duration?
) : TestMetrics {
    /**
     * The run count of this test within the _sliding window_.
     */
    val runCount: Int
        get() = successCount + failureCount + ignoredCount

    /**
     * @see failureRate
     */
    val failureRatePercentage: Int
        get() = when (runCount) {
            0 -> 0

            /*
             * Ignored runs, if any, decrease the failure rate.
             */
            else -> failureCount * 100 / runCount
        }

    /**
     * @see failureRatePercentage
     */
    val failureRate: Double
        get() = failureRatePercentage / 100.0

    /**
     * @see flipRate
     */
    val flipRatePercentage: Int
        get() = when (runCount) {
            0 -> 0

            /*
             * It requires at least two consecutive runs for a single flip.
             * to occur.
             */
            1 -> 0

            else -> {
                val maxFlipCount = runCount - 1

                flipCount * 100 / maxFlipCount
            }
        }

    /**
     * @see flipRatePercentage
     */
    val flipRate: Double
        get() = flipRatePercentage / 100.0
}
