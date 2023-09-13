@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.cvsscalculator

import com.saveourtool.save.cvsscalculator.CvssMetrics.Companion.ATTACK_COMPLEXITY
import com.saveourtool.save.cvsscalculator.CvssMetrics.Companion.ATTACK_VECTOR
import com.saveourtool.save.cvsscalculator.CvssMetrics.Companion.AVAILABILITY
import com.saveourtool.save.cvsscalculator.CvssMetrics.Companion.CONFIDENTIALITY
import com.saveourtool.save.cvsscalculator.CvssMetrics.Companion.CVSS_VERSION
import com.saveourtool.save.cvsscalculator.CvssMetrics.Companion.INTEGRITY
import com.saveourtool.save.cvsscalculator.CvssMetrics.Companion.PRIVILEGES_REQUIRED
import com.saveourtool.save.cvsscalculator.CvssMetrics.Companion.SCOPE
import com.saveourtool.save.cvsscalculator.CvssMetrics.Companion.USER_INTERACTION
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

@Suppress("UtilityClassWithPublicConstructor")
private class CvssMetrics {
    companion object {
        const val ATTACK_COMPLEXITY = "AC"
        const val ATTACK_VECTOR = "AV"
        const val AVAILABILITY = "A"
        const val CONFIDENTIALITY = "C"
        const val CVSS_VERSION = "CVSS"
        const val INTEGRITY = "I"
        const val PRIVILEGES_REQUIRED = "PR"
        const val SCOPE = "S"
        const val USER_INTERACTION = "UI"
    }
}

@Suppress(
    "UnsafeCallOnNullableType",
)
private fun String.parsingVector(): BaseMetrics {
    val values = this.toMap()
    return BaseMetrics(
        version = values.getValue(CVSS_VERSION).toFloat(),
        attackVector = values.findOrElseThrow(ATTACK_VECTOR, AttackVectorType::value),
        attackComplexity = values.findOrElseThrow(ATTACK_COMPLEXITY, AttackComplexityType::value),
        privilegeRequired = values.findOrElseThrow(PRIVILEGES_REQUIRED, PrivilegesRequiredType::value),
        userInteraction = values.findOrElseThrow(USER_INTERACTION, UserInteractionType::value),
        scopeMetric = values.findOrElseThrow(SCOPE, ScopeType::value),
        confidentiality = values.findOrElseThrow(CONFIDENTIALITY, CiaType::value),
        integrity = values.findOrElseThrow(INTEGRITY, CiaType::value),
        availability = values.findOrElseThrow(AVAILABILITY, CiaType::value),
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
private fun roundup(number: Float): Float {
    val value = (number * 100_000).roundToInt()
    return if (value % 10_000 == 0) {
        value / 100_000f
    } else {
        (value / 10_000 + 1) / 10f
    }
}
