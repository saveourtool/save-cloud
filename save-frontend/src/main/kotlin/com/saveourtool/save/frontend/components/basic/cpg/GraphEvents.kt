/**
 * File with component that is used to define the events connected with sigma graph
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.cpg

import com.saveourtool.save.frontend.externals.sigma.*
import js.core.jso
import react.*

val graphEvents: FC<GraphEventsProps> = FC { props ->
    val sigma = useSigma()
    val (hoveredNode, setHoveredNode) = useState<dynamic>(undefined)

    val setSettings = useSetSettings()
    val registerEvents = useRegisterEvents()

    /**
     * effect that makes all the nodes except hoveredNode and its neighbours to be grey
     */
    useEffect(hoveredNode, sigma) {
        setSettings(jso {
            nodeReducer = { node: dynamic, data: dynamic ->
                val graph = sigma.getGraph()
                if (hoveredNode != undefiend) {
                    if (node === hoveredNode || graph.neighbors(hoveredNode).includes(node)) {
                        data["highlighted"] = true
                    } else {
                        data["color"] = "#E2E2E2"
                        data["highlighted"] = false
                        if (props.shouldHideUnfocusedNodes) {
                            data["hidden"] = true
                        }
                    }
                }
                data
            }
            edgeReducer = { edge: dynamic, data: dynamic ->
                val graph = sigma.getGraph()
                if (hoveredNode != undefined && !graph.extremities(edge).includes(hoveredNode)) {
                    data["hidden"] = true
                }
                data
            }
        })
    }

    /*
     * effect that sets the node that is focused by the cursor pointer
     */
    useEffect(hoveredNode, sigma, registerEvents) {
        registerEvents(jso {
            enterNode = { event: dynamic ->
                setHoveredNode(event.node)
                // sigma.getGraph().setNodeAttribute(event.node, "highlighted", true)
            }
            leaveNode = { event: dynamic ->
                setHoveredNode(undefined)
                // sigma.getGraph().removeNodeAttribute(event.node, "highlighted")
            }
        })
    }
}

/**
 * [Props] for [graphEvents] functional component
 */
external interface GraphEventsProps : Props {
    /**
     * Flag that defines whether unfocused nodes should disappear or not
     */
    var shouldHideUnfocusedNodes: Boolean
}
