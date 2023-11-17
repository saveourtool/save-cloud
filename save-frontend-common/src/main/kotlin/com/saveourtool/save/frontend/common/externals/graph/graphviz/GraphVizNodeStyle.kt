package com.saveourtool.save.frontend.common.externals.graph.graphviz

typealias GraphVizStyleSetter<T> = Map<String, () -> T>

/**
 * The React component can take a style configuration object - JSON in the following format
 *
 * format:
 *
 * ```JSON
 * style: {
 *     configPropertyKey1:{
 *         nodeLabel1: "propertyValue1"
 *         nodeLabel2: "propertyValue2"
 *     }
 * }
 * ```
 *
 * example:
 *
 * ```JSON
 * style: {
 *     nodeCaption:{
 *         Movie: "title",
 *         Actor: "name"
 *     }
 * }
 * ```
 */
external interface GraphVizNodeStyle {
    /**
     * Object containing the node labels and the corresponding property to display
     */
    var nodeCaption: GraphVizStyleSetter<String>

    /**
     * Object containing the node labels and the corresponding color to display, in an HTML compatible format
     */
    var nodeColor: GraphVizStyleSetter<String>

    /**
     * Object containing the node labels and the corresponding size to display
     */
    var nodeSize: GraphVizStyleSetter<Number>

    /**
     * Object containing the node labels and the corresponding font size to display for the node caption
     */
    var nodeCaptionSize: GraphVizStyleSetter<Number>
}

/**
 * @param nodeCaption object containing the node labels and the corresponding property to display
 * @param nodeColor object containing the node labels and the corresponding color to display, in an HTML compatible format
 * @param nodeSize object containing the node labels and the corresponding size to display
 * @param nodeCaptionSize object containing the node labels and the corresponding font size to display for the node caption
 * @return [GraphVizNodeStyle] instance that defines the style of [graphVisualization] ([GraphVisualizationProps.style])
 */
@Suppress("FUNCTION_NAME_INCORRECT_CASE", "FunctionNaming")
fun GraphVizNodeStyle(
    nodeCaption: GraphVizStyleSetter<String> = emptyMap(),
    nodeColor: GraphVizStyleSetter<String> = emptyMap(),
    nodeSize: GraphVizStyleSetter<Number> = emptyMap(),
    nodeCaptionSize: GraphVizStyleSetter<Number> = emptyMap(),
) = object : GraphVizNodeStyle {
    override var nodeCaption = nodeCaption
    override var nodeColor = nodeColor
    override var nodeSize = nodeSize
    override var nodeCaptionSize = nodeCaptionSize
}
