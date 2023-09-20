@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.save.cvsscalculator

import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

@Suppress(
    "UnsafeCallOnNullableType",
)
private fun String.parsingVector(): BaseMetrics {
    val values = this.toMap()
    return BaseMetrics(
        version = values.findOrElseThrow(BaseMetricsNames.CVSS_VERSION.value, CvssVersion::value),
        attackVector = values.findOrElseThrow(BaseMetricsNames.ATTACK_VECTOR.value, AttackVectorType::value),
        attackComplexity = values.findOrElseThrow(BaseMetricsNames.ATTACK_COMPLEXITY.value, AttackComplexityType::value),
        privilegeRequired = values.findOrElseThrow(BaseMetricsNames.PRIVILEGES_REQUIRED.value, PrivilegesRequiredType::value),
        userInteraction = values.findOrElseThrow(BaseMetricsNames.USER_INTERACTION.value, UserInteractionType::value),
        scopeMetric = values.findOrElseThrow(BaseMetricsNames.SCOPE.value, ScopeType::value),
        confidentiality = values.findOrElseThrow(BaseMetricsNames.CONFIDENTIALITY.value, CiaType::value),
        integrity = values.findOrElseThrow(BaseMetricsNames.INTEGRITY.value, CiaType::value),
        availability = values.findOrElseThrow(BaseMetricsNames.AVAILABILITY.value, CiaType::value),
    )
}

private inline fun <reified T : Enum<T>> Map<String, String>.findOrElseThrow(vector: String, getValue: (T) -> String): T =
        get(vector)?.let { value -> enumValues<T>().find { getValue(it) == value } }
            ?: throw IllegalArgumentException("No such value for $vector.")

@Suppress("MagicNumber")
private fun String.toMap() = split("/").associate { value ->
    value.split(":")
        .also { require(it.size == 2) }
        .let { it[0] to it[1] }
}

private fun Map<String, Float>.getWeight(key: String): Float = this.getOrElse(key) {
    throw IllegalArgumentException("No such weight for value $key.")
}

/**
 * @param vector
 * @return base score criticality
 */
fun calculateScore(vector: String): Float {
    val baseMetrics = vector.parsingVector()
    return calculate(baseMetrics)
}

/**
 * @param baseMetrics
 * @return base score criticality
 */
fun calculateScore(baseMetrics: BaseMetrics): Float = calculate(baseMetrics)

@Suppress(
    "FLOAT_IN_ACCURATE_CALCULATIONS",
    "MagicNumber",
    "UnsafeCallOnNullableType",
)
private fun calculate(baseMetrics: BaseMetrics): Float {
    val iss = 1f - (1f - cia.getWeight(baseMetrics.confidentiality.value)) * (1f - cia.getWeight(baseMetrics.integrity.value)) * (1f -
            cia.getWeight(baseMetrics.availability.value))
    val impact: Float = if (baseMetrics.scopeMetric == ScopeType.CHANGED) {
        7.52f * (iss - 0.029f) - 3.25f * (iss - 0.02f).pow(15f)
    } else {
        6.42f * iss
    }
    val pr = scope.getOrElse(baseMetrics.scopeMetric.value) { throw IllegalArgumentException("No such weights for Scope type ${baseMetrics.scopeMetric.value}.") }
    val exploitability = 8.22f * av.getWeight(baseMetrics.attackVector.value) * ac.getWeight(baseMetrics.attackComplexity.value) *
            pr.getWeight(baseMetrics.privilegeRequired.value) *
            ui.getWeight(baseMetrics.userInteraction.value)

    val baseScore: Float = if (impact <= 0) {
        0f
    } else {
        if (baseMetrics.scopeMetric == ScopeType.UNCHANGED) {
            min(impact + exploitability, 10f)
        } else {
            min((impact + exploitability) * 1.08f, 10f)
        }
    }

    return roundup(baseScore)
}

@Suppress(
    "FLOAT_IN_ACCURATE_CALCULATIONS",
    "MagicNumber",
)
// https://www.first.org/cvss/v3.1/specification-document#Appendix-A---Floating-Point-Rounding
private fun roundup(number: Float): Float {
    val value = (number * 100_000).roundToInt()
    return if (value % 10_000 == 0) {
        value / 100_000f
    } else {
        (value / 10_000 + 1) / 10f
    }
}
