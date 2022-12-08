package com.saveourtool.save.demo.cpg

import com.saveourtool.save.demo.DemoResult
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * @property cpgGraph graph obtained from Cpg tool run
 * @property logs execution logs
 * @property applicationName query id SQL request to a NEO4
 */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class CpgResult(
    val cpgGraph: CpgGraph,
    val applicationName: String,
    val logs: List<String>,
) : DemoResult {
    companion object {
        val empty = CpgResult(CpgGraph.placeholder, "", emptyList())
    }
}
