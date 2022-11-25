/**
 * File that contains CpgGraph definition
 */

package com.saveourtool.save.demo.cpg

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.ALWAYS
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * @property nodes list of [CpgNode]s
 * @property edges list of [CpgEdge]s
 * @property attributes graph attributes
 * @property options
 */
@Serializable
@ExperimentalSerializationApi
data class CpgGraph(
    val nodes: List<CpgNode> = emptyList(),
    val edges: List<CpgEdge> = emptyList(),
    @EncodeDefault(ALWAYS) val attributes: CpgGraphAttributes = CpgGraphAttributes(),
    @EncodeDefault(ALWAYS) val options: CpgGraphOptions = CpgGraphOptions(),
) {
    /**
     * @return Cpg graph with removed parallel edges
     */
    fun removeMultiEdges(): CpgGraph = edges.groupBy { it.source to it.target }
        .map { (coordinates, parallelEdges) ->
            val newLabel = parallelEdges.joinToString(", ") { it.attributes.label ?: "" }
            val newId = parallelEdges.joinToString(", ") { it.key }
            val newSize = parallelEdges.first().attributes.size
            val paintColor = if (parallelEdges.size > 1) "#FFD5D4" else null
            CpgEdge(
                newId,
                coordinates.first,
                coordinates.second,
                CpgEdgeAttributes(newLabel, paintColor, newSize)
            )
        }
        .let { newEdges ->
            copy(edges = newEdges)
        }

    companion object {
        /**
         * Placeholder of a graph
         */
        val placeholder = CpgGraph(
            nodes = listOf(
                CpgNode("1", CpgNodeAttributes("Alisson", "#FF0000")),
                CpgNode("2", CpgNodeAttributes("John", "#00FF00")),
                CpgNode("3", CpgNodeAttributes("Sam", "#0000FF")),
            ),
            edges = listOf(
                CpgEdge("1->2", "1", "2", CpgEdgeAttributes("KNOWS")),
                CpgEdge("2->3", "2", "3", CpgEdgeAttributes("LIKES")),
                CpgEdge("3->1", "3", "1", CpgEdgeAttributes("HATES")),
            ),
            options = CpgGraphOptions(),
            attributes = CpgGraphAttributes()
        )
    }
}

/**
 * @property name seems to be an id of a graph
 */
@Serializable
@ExperimentalSerializationApi
data class CpgGraphAttributes(
    @EncodeDefault(ALWAYS) val name: String = "Demo graph",
)

/**
 * @property type
 * @property multi
 * @property allowSelfLoops
 */
@Serializable
@ExperimentalSerializationApi
data class CpgGraphOptions(
    @EncodeDefault(ALWAYS) val type: String = "directed",
    @EncodeDefault(ALWAYS) val multi: Boolean = true,
    @EncodeDefault(ALWAYS) val allowSelfLoops: Boolean = true,
)
