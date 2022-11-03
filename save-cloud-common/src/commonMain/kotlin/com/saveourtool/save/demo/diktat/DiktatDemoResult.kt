package com.saveourtool.save.demo.diktat

import kotlinx.serialization.Serializable

/**
 * Class that represents the output of diktat demo
 *
 * @param warnings list of warnings that were found
 * @param outputText resulting file text
 * @property warnings
 * @property outputText
 */
@Serializable
data class DiktatDemoResult(
    val warnings: List<String>,
    val outputText: String,
)
