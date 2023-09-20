@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.cvsscalculator

import kotlinx.serialization.Serializable

/**
 * @property version
 * @property attackVector
 * @property attackComplexity
 * @property privilegeRequired
 * @property userInteraction
 * @property scopeMetric
 * @property confidentiality
 * @property integrity
 * @property availability
 */
@Serializable
data class BaseMetrics(
    var version: CvssVersion,
    var attackVector: AttackVectorType,
    var attackComplexity: AttackComplexityType,
    var privilegeRequired: PrivilegesRequiredType,
    var userInteraction: UserInteractionType,
    var scopeMetric: ScopeType,
    var confidentiality: CiaType,
    var integrity: CiaType,
    var availability: CiaType,
) {
    /**
     * @return true if BaseMetrics is valid, false otherwise
     */
    fun isValid(): Boolean = attackVector != AttackVectorType.NOT_DEFINED && attackComplexity != AttackComplexityType.NOT_DEFINED &&
            privilegeRequired != PrivilegesRequiredType.NOT_DEFINED && userInteraction != UserInteractionType.NOT_DEFINED &&
            scopeMetric != ScopeType.NOT_DEFINED && confidentiality != CiaType.NOT_DEFINED &&
            integrity != CiaType.NOT_DEFINED && availability != CiaType.NOT_DEFINED

    /**
     * @return severity score vector
     */
    fun scoreVectorString() =
            "${BaseMetricsNames.CVSS_VERSION.value}:${version.value}/${BaseMetricsNames.ATTACK_VECTOR.value}:${attackVector.value}/" +
                    "${BaseMetricsNames.ATTACK_COMPLEXITY.value}:${attackComplexity.value}/${BaseMetricsNames.PRIVILEGES_REQUIRED.value}:" +
                    "${privilegeRequired.value}/${BaseMetricsNames.USER_INTERACTION.value}:${userInteraction.value}/${BaseMetricsNames.SCOPE.value}:" +
                    "${scopeMetric.value}/${BaseMetricsNames.CONFIDENTIALITY.value}:${confidentiality.value}/${BaseMetricsNames.INTEGRITY.value}:" +
                    "${integrity.value}/${BaseMetricsNames.AVAILABILITY.value}:${availability.value}"

    companion object {
        val empty = BaseMetrics(
            version = CvssVersion.THREE_ONE,
            attackVector = AttackVectorType.NOT_DEFINED,
            attackComplexity = AttackComplexityType.NOT_DEFINED,
            privilegeRequired = PrivilegesRequiredType.NOT_DEFINED,
            userInteraction = UserInteractionType.NOT_DEFINED,
            scopeMetric = ScopeType.NOT_DEFINED,
            confidentiality = CiaType.NOT_DEFINED,
            integrity = CiaType.NOT_DEFINED,
            availability = CiaType.NOT_DEFINED,
        )
    }
}

/**
 * Names of base metrics
 *
 * @property value abbreviated value
 */
@Serializable
enum class BaseMetricsNames(val value: String) {
    ATTACK_COMPLEXITY("AC"),
    ATTACK_VECTOR("AV"),
    AVAILABILITY("A"),
    CONFIDENTIALITY("C"),
    CVSS_VERSION("CVSS"),
    INTEGRITY("I"),
    PRIVILEGES_REQUIRED("PR"),
    SCOPE("S"),
    USER_INTERACTION("UI"),
    ;
}
