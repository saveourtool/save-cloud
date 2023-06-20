@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.graph

import com.saveourtool.save.demo.cpg.CpgGraph
import com.saveourtool.save.demo.cpg.cytoscape.CytoscapeLayout
import com.saveourtool.save.frontend.externals.graph.asCytoscapeGraph
import com.saveourtool.save.frontend.externals.graph.cytoscape.cytoscape
import react.*

val cytoscapeVisualizer: FC<CytoscapeVisualizerProps> = FC { props ->
    val cytoscapeJs = cytoscape(props.graph.asCytoscapeGraph(), props.layout)
    if (cytoscapeJs != undefined) {
        /*
         * TODO: implement event handling
         *
         * Events should be handled like this:
         *
         * cytoscapeJs.bind("tap") { event -> val node = event.target }
         *
         * TODO: Now there is a problem - graph is being re-rendered on state change, which is a problem
         * because no node movements are remembered.
         */
        Unit
    }
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
