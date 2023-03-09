package com.saveourtool.save.test

import kotlinx.serialization.Serializable

/**
 * @property checkId the unique check id.
 * @property checkName the human-readable check name.
 * @property percentage the completion percentage (`0..100`).
 */
@Serializable
data class TestSuiteValidationResult(
    val checkId: String,
    val checkName: String,
    val percentage: Int
) {
    init {
        @Suppress("MAGIC_NUMBER")
        require(percentage in 0..100) {
            percentage.toString()
        }
    }

    override fun toString(): String =
            when (percentage) {
                100 -> "Check $checkName is complete."
                else -> "Check $checkName is running, $percentage% complete\u2026"
            }
}
