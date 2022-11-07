package com.saveourtool.save.demo.diktat

import com.saveourtool.save.demo.DemoResult
import kotlinx.serialization.Serializable

/**
 * Class that represents the output of diktat demo
 *
 * @property warnings list of warnings that were found
 * @property outputText resulting file text
 */
@Serializable
data class DiktatDemoResult(
    val warnings: List<String>,
    val outputText: String,
) : DemoResult
