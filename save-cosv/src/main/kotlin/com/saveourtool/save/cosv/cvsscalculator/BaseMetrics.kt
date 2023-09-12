package com.saveourtool.save.cosv.cvsscalculator

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
    var attackVector: String,
    var attackComplexity: String,
    var privilegeRequired: String,
    var userInteraction: String,
    var scopeMetric: String,
    var confidentiality: String,
    var integrity: String,
    var availability: String,
)
