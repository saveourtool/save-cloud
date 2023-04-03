/**
 * Utils for CpgGraph and Sigma in general
 */

package com.saveourtool.save.frontend.externals.sigma

import com.saveourtool.save.demo.cpg.CpgEdge
import com.saveourtool.save.demo.cpg.CpgGraph
import com.saveourtool.save.demo.cpg.CpgNode

import js.core.jso

import kotlin.random.Random
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
 * Paint [CpgNode] with color
 *
 * @param color color to paint the [CpgNode] to
 * @return [CpgNode] painted with [color]
 */
fun CpgNode.paint(color: String) = copy(
    attributes = attributes.copy(color = color)
)

/**
 * Paint [CpgEdge] with color
 *
 * @param color color to paint the [CpgEdge] to
 * @return [CpgEdge] painted with [color]
 */
fun CpgEdge.paint(color: String) = copy(
    attributes = attributes.copy(color = color)
)

/**
 * Paint all the [CpgNode]s of [CpgGraph] with [color].
 *
 * @param color color to paint the [CpgGraph.nodes] to, if null - each [CpgNode] is painted into random color
 * @return [CpgGraph] with all [CpgGraph.nodes] painted with [color] (or randomly)
 */
fun CpgGraph.paintNodes(
    color: String? = null
) = nodes.map { node ->
    node.paint(color ?: getRandomHexColor())
}
    .let { coloredNodes ->
        copy(nodes = coloredNodes)
    }

/**
 * Paint all the [CpgEdge]s of [CpgGraph] with [color].
 *
 * @param color color to paint the [CpgGraph.edges] to, if null - each [CpgEdge] is painted into random color
 * @return [CpgGraph] with all [CpgGraph.edges] painted with [color] (or randomly)
 */
fun CpgGraph.paintEdges(
    color: String? = null
) = edges.map { edge ->
    edge.paint(color ?: getRandomHexColor())
}
    .let { coloredEdges ->
        copy(edges = coloredEdges)
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

/**
 * Get random hex color
 *
 * @param isPastel flag to generate only pastel colors - true by default
 * @return randomized color in format "#RRGGBB"
 */
@Suppress("MAGIC_NUMBER")
fun getRandomHexColor(isPastel: Boolean = true) = buildString {
    append("#")
    val from = if (isPastel) 127 else 0
    append(Random.nextInt(from, 255).toString(16))
    append(Random.nextInt(from, 255).toString(16))
    append(Random.nextInt(from, 255).toString(16))
}
