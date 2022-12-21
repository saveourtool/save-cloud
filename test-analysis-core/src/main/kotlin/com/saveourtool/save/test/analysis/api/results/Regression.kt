package com.saveourtool.save.test.analysis.api.results

/**
 * @property detailMessage the detail message string of this result.
 */
data class Regression(override val detailMessage: String = "") : IrregularTest
