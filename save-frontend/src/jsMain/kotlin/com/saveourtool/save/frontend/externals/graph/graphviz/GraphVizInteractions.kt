package com.saveourtool.save.frontend.externals.graph.graphviz

import web.events.Event

typealias GraphVizEventHandler = (event: Event) -> Unit

/**
 * The React component can take an interactions configuration object - JSON in the following format:
 *
 * ```JSON
 * interactions: {
 *     eventName: (e) => callback method
 * }
 * ```
 *
 * Example:
 *
 * ```JSON
 * interactions: {
 *     onNodeClick: (e) => alert(e.name)
 * }
 * ```
 */
external interface GraphVizInteractions {
    /**
     * Triggered when a node is clicked
     */
    var onNodeClick: (event: Event) -> Unit

    /**
     * Triggered when a node is right-clicked
     */
    var onNodeRightClick: (event: Event) -> Unit
}

/**
 * @param onNodeClick handler that is triggered when a node is clicked
 * @param onNodeRightClick handler that is triggered when a node is right-clicked
 * @return [GraphVizInteractions] instance that defines the interactions of [graphVisualization] ([GraphVisualizationProps.interactions])
 */
@Suppress("FUNCTION_NAME_INCORRECT_CASE", "FunctionNaming")
fun GraphVizInteractions(
    onNodeClick: GraphVizEventHandler = {},
    onNodeRightClick: GraphVizEventHandler = {},
) = object : GraphVizInteractions {
    override var onNodeClick: (event: Event) -> Unit = onNodeClick
    override var onNodeRightClick: (event: Event) -> Unit = onNodeRightClick
}
