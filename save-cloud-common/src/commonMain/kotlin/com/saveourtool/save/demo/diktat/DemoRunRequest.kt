package com.saveourtool.save.demo.diktat

import com.saveourtool.save.demo.DemoRequest
import kotlinx.serialization.Serializable

/**
 * @property codeLines file as String that contains code requested for diktat run
 * @property params all the params required for run
 */
@Serializable
data class DemoRunRequest(
    val codeLines: List<String>,
    val params: DemoAdditionalParams,
) : DemoRequest
