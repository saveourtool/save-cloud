package com.saveourtool.save.frontend.externals.graph.graphviz

import com.saveourtool.save.frontend.components.basic.graph.graphVizVisualizer

/**
 * Enum class that represents available layouts for [graphVizVisualizer]
 * @property layoutName
 */
enum class GraphVizLayout(val layoutName: String) {
    /**
     * Layout for graphs
     */
    GRAPH("graph"),

    /**
     * Layout for tree-like representation of graph
     */
    TREE("tree"),
    ;

    companion object {
        val preferredLayout = GRAPH
    }
}
