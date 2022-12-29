package com.saveourtool.save.test.analysis.api.results

/**
 * @property detailMessage the detail message string of this result.
 */
data class FlakyTest(override val detailMessage: String = "") : IrregularTest
