@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.graph

import com.saveourtool.save.demo.cpg.CpgGraph
import com.saveourtool.save.demo.cpg.cytoscape.CytoscapeLayout
import com.saveourtool.save.frontend.externals.graph.asCytoscapeGraph
import com.saveourtool.save.frontend.externals.graph.cytoscape.cytoscape
import react.FC
import react.Props

val cytoscapeVisualizer: FC<CytoscapeVisualizerProps> = FC { props ->
    cytoscape(
        props.graph.asCytoscapeGraph(),
        props.layout
    )
}

/**
 * [Props] for [cytoscapeVisualizer]
 */
external interface CytoscapeVisualizerProps : Props {
    /**
     * Graph to render, must be set
     */
    var graph: CpgGraph

    /**
     * [CytoscapeLayout] to apply to graph, must be set
     */
    var layout: CytoscapeLayout
}
