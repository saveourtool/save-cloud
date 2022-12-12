package com.saveourtool.save.demo.diktat

import com.saveourtool.save.demo.DemoRequest
import kotlinx.serialization.Serializable

/**
 * @property codeLines file as String that contains code requested for diktat demo run
 * @property params all the params required for diktat demo run
 */
@Serializable
data class DiktatRunRequest(
    val codeLines: List<String>,
    val params: DiktatAdditionalParams,
) : DemoRequest
