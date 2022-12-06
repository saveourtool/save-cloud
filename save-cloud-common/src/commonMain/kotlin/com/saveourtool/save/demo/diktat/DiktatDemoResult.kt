package com.saveourtool.save.demo.diktat

import com.saveourtool.save.demo.DemoResult
import kotlinx.serialization.Serializable

/**
 * Class that represents the output of diktat demo
 *
 * @property warnings list of warnings that were found
 * @property outputText resulting file text
 * @property logs launch logs
 * @property terminationCode termination code of a tool running process
 */
@Serializable
data class DiktatDemoResult(
    val warnings: List<String>,
    val outputText: String,
    val logs: List<String>,
    val terminationCode: Int,
) : DemoResult {
    companion object {
        val empty = DiktatDemoResult(emptyList(), "", emptyList(), -1)
    }
}
