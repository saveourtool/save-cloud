package com.saveourtool.save.test.analysis.metrics

import kotlinx.serialization.Serializable

/**
 * Scalar test metrics returned by `TestStatisticsStorage.getTestMetrics`.
 */
@Serializable
sealed interface TestMetrics
