package com.saveourtool.save.test.analysis.api.results

import com.saveourtool.save.test.analysis.api.metrics.RegularTestMetrics

/**
 * A permanent failure &mdash; a test that has a 100%
 * [failure rate][RegularTestMetrics.failureRate].
 *
 * @property detailMessage the detail message string of this result.
 * @see RegularTestMetrics.failureRate
 */
data class PermanentFailure(override val detailMessage: String = "") : IrregularTest
