@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.graph

import com.saveourtool.save.frontend.externals.graph.graphviz.GraphVizLayout
import com.saveourtool.save.frontend.externals.graph.graphviz.GraphVizRenderer
import com.saveourtool.save.frontend.externals.graph.graphviz.graphVisualization
import react.FC
import react.Props

val graphVizVisualizer: FC<GraphVizVisualizerProps> = FC { props ->
    graphVisualization {
        data = props.graph
        renderer = GraphVizRenderer.preferredRenderer.rendererName
        layout = (props.layout ?: GraphVizLayout.preferredLayout).layoutName
    }
}

/**
 * [Props] for [graphVizVisualizer]
 */
external interface GraphVizVisualizerProps : Props {
    /**
     * Graph to render
     */
    var graph: dynamic

    /**
     * [GraphVizLayout] to apply to graph
     */
    var layout: GraphVizLayout?
}
