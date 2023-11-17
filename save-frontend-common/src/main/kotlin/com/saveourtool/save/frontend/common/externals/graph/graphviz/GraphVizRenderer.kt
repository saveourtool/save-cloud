package com.saveourtool.save.frontend.common.externals.graph.graphviz

import com.saveourtool.save.frontend.common.components.basic.graph.graphVizVisualizer

/**
 * Enum class that represents available renderers for [graphVizVisualizer]
 *
 * @property rendererName real renderer name that should be passed to library
 */
enum class GraphVizRenderer(val rendererName: String) {
    /**
     * Default renderer
     */
    CYTOSCAPE("cytoscape"),

    /**
     * Some other renderer
     */
    REACT_FORCE_GRAPH("react-force-graph"),
    ;

    companion object {
        val preferredRenderer = CYTOSCAPE
    }
}
