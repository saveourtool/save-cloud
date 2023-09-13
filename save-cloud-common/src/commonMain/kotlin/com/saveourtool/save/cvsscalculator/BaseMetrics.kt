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
    var version: Float,
    var attackVector: AttackVectorType,
    var attackComplexity: AttackComplexityType,
    var privilegeRequired: PrivilegesRequiredType,
    var userInteraction: UserInteractionType,
    var scopeMetric: ScopeType,
    var confidentiality: CiaType,
    var integrity: CiaType,
    var availability: CiaType,
)
