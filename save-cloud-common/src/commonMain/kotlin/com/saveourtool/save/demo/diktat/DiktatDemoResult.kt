package com.saveourtool.save.demo.diktat

import com.saveourtool.save.demo.DemoResult
import kotlinx.serialization.Serializable

/**
 * Class that represents the output of diktat demo
 *
 * @property warnings list of warnings that were found
 * @property outputText list of lines of resulting file
 * @property logs launch logs
 * @property terminationCode termination code of a tool running process
 */
@Serializable
data class DiktatDemoResult(
    val warnings: List<String>,
    val outputText: List<String>,
    val logs: List<String>,
    val terminationCode: Int,
) : DemoResult {
    companion object {
        val empty = DiktatDemoResult(emptyList(), emptyList(), emptyList(), -1)
    }
}
