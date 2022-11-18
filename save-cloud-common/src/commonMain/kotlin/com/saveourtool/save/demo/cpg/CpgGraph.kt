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
 */
@Serializable
@ExperimentalSerializationApi
data class CpgGraph(
    val nodes: List<CpgNode>,
    val edges: List<CpgEdge>,
    @EncodeDefault(ALWAYS) val attributes: CpgGraphAttributes = CpgGraphAttributes(),
) {
    companion object {
        /**
         * Placeholder of a graph
         */
        val placeholder = CpgGraph(
            listOf(
                CpgNode(
                    "1",
                    CpgNodeAttributes("Alisson", "#FF0000"),
                ),
                CpgNode(
                    "2",
                    CpgNodeAttributes("John", "#00FF00"),
                ),
                CpgNode(
                    "3",
                    CpgNodeAttributes("Sam", "#0000FF"),
                ),
            ),
            listOf(
                CpgEdge(
                    "1->2",
                    "1",
                    "2",
                    CpgEdgeAttributes("KNOWS"),
                ),
                CpgEdge(
                    "2->3",
                    "2",
                    "3",
                    CpgEdgeAttributes("LIKES"),
                ),
                CpgEdge(
                    "3->1",
                    "3",
                    "1",
                    CpgEdgeAttributes("HATES"),
                ),
            ),
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
