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
    fun isValid(): Boolean = attackVector != AttackVectorType.NOT_DEFINED && attackComplexity != AttackComplexityType.NOT_DEFINED && privilegeRequired != PrivilegesRequiredType.NOT_DEFINED &&
            userInteraction != UserInteractionType.NOT_DEFINED && scopeMetric != ScopeType.NOT_DEFINED && confidentiality != CiaType.NOT_DEFINED && integrity != CiaType.NOT_DEFINED && availability != CiaType.NOT_DEFINED

    /**
     * @return severity score vector
     */
    fun scoreVectorString() =
            "$CVSS_VERSION:${version.value}/$ATTACK_VECTOR:${attackVector.value}/" +
                    "$ATTACK_COMPLEXITY:${attackComplexity.value}/$PRIVILEGES_REQUIRED:${privilegeRequired.value}/" +
                    "$USER_INTERACTION:${userInteraction.value}/$SCOPE:${scopeMetric.value}/" +
                    "$CONFIDENTIALITY:${confidentiality.value}/$INTEGRITY:${integrity.value}/$AVAILABILITY:${availability.value}"

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
