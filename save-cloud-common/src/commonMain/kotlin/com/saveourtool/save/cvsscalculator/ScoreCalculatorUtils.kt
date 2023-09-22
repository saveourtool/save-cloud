@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.save.cvsscalculator

import kotlin.math.roundToInt

/**
 * @param vector severity score vector
 * @param getValue
 * @return value
 */
inline fun <reified T : Enum<T>> Map<String, String>.findOrElseThrow(vector: String, getValue: (T) -> String): T =
        get(vector)?.let { value -> enumValues<T>().find { getValue(it) == value } }
            ?: throw IllegalArgumentException("No such value for $vector.")

/**
 * @return map of vector values
 */
@Suppress("MagicNumber")
fun String.parsingVectorToMap() = split("/").associate { value ->
    value.split(":")
        .also { require(it.size == 2) }
        .let { it[0] to it[1] }
}

/**
 * @param key of severity score vector
 * @return weight for vector value
 */
fun Map<String, Float>.getWeight(key: String): Float = this.getOrElse(key) {
    throw IllegalArgumentException("No such weight for value $key.")
}

/**
 * @param number number to round
 * @return Rounds a value up to one decimal place
 */
@Suppress(
    "FLOAT_IN_ACCURATE_CALCULATIONS",
    "MagicNumber",
)
// https://www.first.org/cvss/v3.1/specification-document#Appendix-A---Floating-Point-Rounding
fun roundup(number: Float): Float {
    val value = (number * 100_000).roundToInt()
    return if (value % 10_000 == 0) {
        value / 100_000f
    } else {
        (value / 10_000 + 1) / 10f
    }
}
