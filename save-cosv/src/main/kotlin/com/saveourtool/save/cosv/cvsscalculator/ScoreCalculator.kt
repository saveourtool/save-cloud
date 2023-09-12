@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.cosv.cvsscalculator

import com.saveourtool.save.cosv.cvsscalculator.CvssMetrics.Companion.ATTACK_COMPLEXITY
import com.saveourtool.save.cosv.cvsscalculator.CvssMetrics.Companion.ATTACK_VECTOR
import com.saveourtool.save.cosv.cvsscalculator.CvssMetrics.Companion.AVAILABILITY
import com.saveourtool.save.cosv.cvsscalculator.CvssMetrics.Companion.CONFIDENTIALITY
import com.saveourtool.save.cosv.cvsscalculator.CvssMetrics.Companion.CVSS_VERSION
import com.saveourtool.save.cosv.cvsscalculator.CvssMetrics.Companion.INTEGRITY
import com.saveourtool.save.cosv.cvsscalculator.CvssMetrics.Companion.PRIVILEGES_REQUIRED
import com.saveourtool.save.cosv.cvsscalculator.CvssMetrics.Companion.SCOPE
import com.saveourtool.save.cosv.cvsscalculator.CvssMetrics.Companion.USER_INTERACTION
import java.lang.Float.min
import kotlin.math.pow
import kotlin.math.roundToInt

private class CvssMetrics {
    companion object {
        const val ATTACK_COMPLEXITY = "/AC:"
        const val ATTACK_VECTOR = "/AV:"
        const val AVAILABILITY = "/A:"
        const val CONFIDENTIALITY = "/C:"
        const val CVSS_VERSION = "CVSS:"
        const val INTEGRITY = "/I:"
        const val PRIVILEGES_REQUIRED = "/PR:"
        const val SCOPE = "/S:"
        const val USER_INTERACTION = "/UI:"
    }
}

private fun String.parsingVector(): BaseMetrics = BaseMetrics(
    version = this.getValue(CVSS_VERSION).toFloat(),
    attackVector = this.getValue(ATTACK_VECTOR),
    attackComplexity = this.getValue(ATTACK_COMPLEXITY),
    privilegeRequired = this.getValue(PRIVILEGES_REQUIRED),
    userInteraction = this.getValue(USER_INTERACTION),
    scopeMetric = this.getValue(SCOPE),
    confidentiality = this.getValue(CONFIDENTIALITY),
    integrity = this.getValue(INTEGRITY),
    availability = this.getValue(AVAILABILITY),
)

private fun String.getValue(index: String) = substringAfter(index).substringBefore("/")

/**
 * @param vector
 * @return base score criticality
 */
fun scoreCalculator(vector: String): Float {
    val baseMetrics = vector.parsingVector()
    return calculate(baseMetrics)
}

@Suppress("FLOAT_IN_ACCURATE_CALCULATIONS")
private fun calculate(baseMetrics: BaseMetrics): Float {
    val iss = 1f - (1f - cia[baseMetrics.confidentiality]!!) * (1f - cia[baseMetrics.integrity]!!) * (1f - cia[baseMetrics.availability]!!)
    val impact: Float = if (baseMetrics.scopeMetric == "C") {
        7.52f * (iss - 0.029f) - 3.25f * (iss - 0.02f).pow(15f)
    } else {
        6.42f * iss
    }
    val pr = scope[baseMetrics.scopeMetric]!!
    val exploitability = 8.22f * av[baseMetrics.attackVector]!! * ac[baseMetrics.attackComplexity]!! * pr[baseMetrics.privilegeRequired]!! * ui[baseMetrics.userInteraction]!!

    val baseScore: Float = if (impact <= 0) {
        0f
    } else if (baseMetrics.scopeMetric == "U") {
        min(impact + exploitability, 10f)
    } else {
        min((impact + exploitability) * 1.08f, 10f)
    }

    return roundup(baseScore)
}

@Suppress("FLOAT_IN_ACCURATE_CALCULATIONS")
private fun roundup(number: Float): Float {
    val value = (number * 100_000).roundToInt()
    return if (value % 10_000 == 0) {
        value / 100_000f
    } else {
        (value / 10_000 + 1) / 10f
    }
}
