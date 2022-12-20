package com.saveourtool.save.test.analysis.api.metrics

import com.saveourtool.save.test.analysis.api.TestStatisticsStorage

/**
 * Scalar test metrics returned by [TestStatisticsStorage.getTestMetrics].
 *
 * @see TestStatisticsStorage.getTestMetrics
 */
sealed interface TestMetrics
