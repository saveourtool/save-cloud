/**
 * Utils for CpgGraph and Sigma in general
 */
package com.saveourtool.save.frontend.externals.sigma

import com.saveourtool.save.demo.cpg.CpgGraph
import kotlinx.js.jso
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val cpgJsonSerializer = Json { encodeDefaults = true }

/**
 * @return serialized graph that can be used with useLoadGraph hook
 */
fun CpgGraph.toJson() = let { graph ->
    val str = cpgJsonSerializer.encodeToString(graph)
    js("JSON.parse(str);")
}

/**
 * @param edgeType type of the edge that should be displayed
 * @param isRenderEdgeLabels flag that defines if edge labels should be displayed or not
 * @return settings of sigmaContainer
 */
fun getSigmaContainerSettings(
    edgeType: String = "arrow",
    isRenderEdgeLabels: Boolean = true,
): dynamic = jso {
    renderEdgeLabels = isRenderEdgeLabels
    defaultEdgeType = edgeType
}
