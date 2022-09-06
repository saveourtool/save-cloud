/**
 * Utilities for execution score calculation
 */

@file:Suppress("MagicNumber", "MAGIC_NUMBER", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.utils

import com.saveourtool.save.execution.ExecutionDto
import com.saveourtool.save.execution.TestingType

/**
 * Set of types for different ways of score calculation
 */
enum class ScoreType {
    // TODO: what else algorithms do we need?

    /** Calculate score via F-measure.  */
    F_MEASURE,
    ;
}

/**
 * @return precision rate
 */
fun ExecutionDto.getPrecisionRate() = calculateRate(matchedChecks, matchedChecks + unexpectedChecks) ?: 0

/**
 * @return recall rate
 */
fun ExecutionDto.getRecallRate() = calculateRate(matchedChecks, matchedChecks + unmatchedChecks) ?: 0

/**
 * @param scoreType
 * @return score according execution [type] and [scoreType]
 */
fun ExecutionDto.calculateScore(scoreType: ScoreType): Double = when (type) {
    TestingType.CONTEST_MODE -> calculateScoreForContestMode(scoreType)
    // TODO: how to calculate score for other types?
    else -> 0.0
}

private fun ExecutionDto.calculateScoreForContestMode(scoreType: ScoreType): Double = when (scoreType) {
    ScoreType.F_MEASURE -> calculateFmeasure()
    else -> TODO("Invalid score type for contest mode!")
}

private fun ExecutionDto.calculateFmeasure(): Double {
    val denominator = getPrecisionRate() + getRecallRate()
    return if (denominator == 0) {
        0.0
    } else {
        (2 * getPrecisionRate() * getRecallRate()) / (getPrecisionRate() + getRecallRate()).toDouble()
    }
}

/**
 * @param numerator
 * @param denominator
 * @return rate based on [numerator] and [denominator]
 */
fun calculateRate(numerator: Long, denominator: Long) = denominator.takeIf { it > 0 }
    ?.run { numerator.toDouble() / denominator }
    ?.let { it * 100 }
    ?.toInt()
