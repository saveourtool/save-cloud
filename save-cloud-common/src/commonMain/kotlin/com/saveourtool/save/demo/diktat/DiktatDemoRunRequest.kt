package com.saveourtool.save.demo.diktat

import kotlinx.serialization.Serializable

/**
 * @property codeLines file as String that contains code requested for diktat run
 * @property params all the params required for run
 */
@Serializable
data class DiktatDemoRunRequest(
    val codeLines: String,
    val params: DiktatDemoAdditionalParams,
)
