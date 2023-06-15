/**
 * File with component that is used to define the events connected with sigma graph
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.cpg

import com.saveourtool.save.frontend.externals.graph.sigma.useRegisterEvents
import com.saveourtool.save.frontend.externals.graph.sigma.useSetSettings
import com.saveourtool.save.frontend.externals.graph.sigma.useSigma
import js.core.jso
import react.*

/**
 * Component that defines event handlers for CPG graph
 */
val graphEvents: FC<GraphEventsProps> = FC { props ->
    val sigma = useSigma()
    val (hoveredNode, setHoveredNode) = useState<dynamic>(undefined)

    val setSettings = useSetSettings()
    val registerEvents = useRegisterEvents()

    /*
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
    useEffect(registerEvents, hoveredNode, sigma) {
        registerEvents(jso {
            enterNode = { event: dynamic ->
                setHoveredNode(event.node)
            }
            leaveNode = { event: dynamic ->
                setHoveredNode(undefined)
            }
            clickNode = { event: dynamic ->
                props.setSelectedNode(event.node)
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

    /**
     * Callback to update the node that was selected by user
     */
    var setSelectedNode: (String) -> Unit
}
