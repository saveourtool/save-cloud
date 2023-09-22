@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.save.cvsscalculator

import com.saveourtool.save.cvsscalculator.v2.*
import com.saveourtool.save.cvsscalculator.v3.*
import kotlin.math.min
import kotlin.math.pow

const val CVSS_VERSION = "CVSS"

/**
 * @param vector
 * @return base score criticality
 */
fun calculateScore(vector: String): Float {
    val map = vector.parsingVectorToMap()

    return when (map.findOrElseThrow(CVSS_VERSION, CvssVersion::value)) {
        CvssVersion.CVSS_V2 -> calculate(getBaseMetricsV2(map))
        CvssVersion.CVSS_V3, CvssVersion.CVSS_V3_1 -> calculate(getBaseMetricsV3(map))
    }
}

/**
 * @param baseMetricsV3
 * @return base score criticality
 */
fun calculateScore(baseMetricsV3: BaseMetricsV3): Float = calculate(baseMetricsV3)

/**
 * @param baseMetricsV2
 * @return base score criticality
 */
fun calculateScore(baseMetricsV2: BaseMetricsV2): Float = calculate(baseMetricsV2)

@Suppress(
    "UnsafeCallOnNullableType",
)
private fun getBaseMetricsV3(map: Map<String, String>): BaseMetricsV3 = BaseMetricsV3(
    version = map.findOrElseThrow(BaseMetricsV3Names.CVSS_VERSION.value, CvssVersion::value),
    attackVector = map.findOrElseThrow(BaseMetricsV3Names.ATTACK_VECTOR.value, AttackVectorType::value),
    attackComplexity = map.findOrElseThrow(BaseMetricsV3Names.ATTACK_COMPLEXITY.value, AttackComplexityType::value),
    privilegeRequired = map.findOrElseThrow(BaseMetricsV3Names.PRIVILEGES_REQUIRED.value, PrivilegesRequiredType::value),
    userInteraction = map.findOrElseThrow(BaseMetricsV3Names.USER_INTERACTION.value, UserInteractionType::value),
    scopeMetric = map.findOrElseThrow(BaseMetricsV3Names.SCOPE.value, ScopeType::value),
    confidentiality = map.findOrElseThrow(BaseMetricsV3Names.CONFIDENTIALITY.value, CiaType::value),
    integrity = map.findOrElseThrow(BaseMetricsV3Names.INTEGRITY.value, CiaType::value),
    availability = map.findOrElseThrow(BaseMetricsV3Names.AVAILABILITY.value, CiaType::value),
)

@Suppress(
    "UnsafeCallOnNullableType",
)
private fun getBaseMetricsV2(map: Map<String, String>): BaseMetricsV2 = BaseMetricsV2(
    version = map.findOrElseThrow(BaseMetricsV2Names.CVSS_VERSION.value, CvssVersion::value),
    accessVector = map.findOrElseThrow(BaseMetricsV2Names.ACCESS_VECTOR.value, AccessVectorType::value),
    accessComplexity = map.findOrElseThrow(BaseMetricsV2Names.ACCESS_COMPLEXITY.value, AccessComplexityType::value),
    authentication = map.findOrElseThrow(BaseMetricsV2Names.AUTHENTICATION.value, AuthenticationType::value),
    confidentiality = map.findOrElseThrow(BaseMetricsV2Names.CONFIDENTIALITY.value, CiaTypeV2::value),
    integrity = map.findOrElseThrow(BaseMetricsV2Names.INTEGRITY.value, CiaTypeV2::value),
    availability = map.findOrElseThrow(BaseMetricsV2Names.AVAILABILITY.value, CiaTypeV2::value),
)

@Suppress(
    "FLOAT_IN_ACCURATE_CALCULATIONS",
    "MagicNumber",
    "UnsafeCallOnNullableType",
)
private fun calculate(baseMetricsV3: BaseMetricsV3): Float {
    val iss = 1f - (1f - cia.getWeight(baseMetricsV3.confidentiality.value)) * (1f - cia.getWeight(baseMetricsV3.integrity.value)) * (1f -
            cia.getWeight(baseMetricsV3.availability.value))
    val impact: Float = if (baseMetricsV3.scopeMetric == ScopeType.CHANGED) {
        7.52f * (iss - 0.029f) - 3.25f * (iss - 0.02f).pow(15f)
    } else {
        6.42f * iss
    }
    val pr = scope.getOrElse(baseMetricsV3.scopeMetric.value) { throw IllegalArgumentException("No such weights for Scope type ${baseMetricsV3.scopeMetric.value}.") }
    val exploitability = 8.22f * av.getWeight(baseMetricsV3.attackVector.value) * ac.getWeight(baseMetricsV3.attackComplexity.value) *
            pr.getWeight(baseMetricsV3.privilegeRequired.value) *
            ui.getWeight(baseMetricsV3.userInteraction.value)

    val baseScore: Float = if (impact <= 0) {
        0f
    } else {
        if (baseMetricsV3.scopeMetric == ScopeType.UNCHANGED) {
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
    "UnsafeCallOnNullableType",
)
private fun calculate(baseMetricsV2: BaseMetricsV2): Float {
    val impact = 10.41f * (1f - (1f - ciaImpact.getWeight(baseMetricsV2.confidentiality.value)) * (1f - ciaImpact.getWeight(baseMetricsV2.integrity.value)) * (1f -
            ciaImpact.getWeight(baseMetricsV2.availability.value)))

    val exploitability = 20f * accessV.getWeight(baseMetricsV2.accessVector.value) * accessC.getWeight(baseMetricsV2.accessComplexity.value) *
            auth.getWeight(baseMetricsV2.authentication.value)

    val impact = if (impact == 0f) 0f else 1.176f

    val baseScore: Float = ((0.6f * impact) + (0.4f * exploitability) - 1.5f) * fImpact

    return roundup(baseScore)
}
