@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.graph

import com.saveourtool.save.demo.cpg.CpgGraph
import com.saveourtool.save.demo.cpg.cytoscape.CytoscapeLayout
import com.saveourtool.save.frontend.externals.graph.asCytoscapeGraph
import com.saveourtool.save.frontend.externals.graph.cytoscape.cytoscape
import js.core.jso
import react.*
import react.dom.html.ReactHTML.div
import web.cssom.Height
import web.cssom.Width
import web.html.HTMLDivElement

val cytoscapeVisualizer: FC<CytoscapeVisualizerProps> = FC { props ->
    val divRef: MutableRefObject<HTMLDivElement> = useRef(null)
    val graph = useMemo(props.graph) { props.graph.asCytoscapeGraph() }
    val cytoscapeJs = useMemo(graph, props.layout) {
        cytoscape(graph, props.layout, divRef)
    }

    val (selectedNode, setSelectedNode) = useState<dynamic>(undefined)

    useEffect {
        if (cytoscapeJs != undefined) {
            cytoscapeJs.bind("tap", "node") { event ->
                val clickedNode = event.target

                if (selectedNode != clickedNode) {
                    if (selectedNode != undefined) {
                        cytoscapeJs.nodes().show()
                    }
                    val neighbors = clickedNode.neighborhood().add(clickedNode)
                    cytoscapeJs.nodes().forEach { node ->
                        val isInNeighbours: Boolean = neighbors.has(node) as Boolean
                        if (!isInNeighbours) {
                            node.hide()
                        }
                    }
                    setSelectedNode { clickedNode }
                } else {
                    cytoscapeJs.nodes().show()
                    setSelectedNode { undefined }
                }
            }
        }
    }
    div {
        ref = divRef
        style = jso {
            width = "100%".unsafeCast<Width>()
            height = "90%".unsafeCast<Height>()
        }
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
