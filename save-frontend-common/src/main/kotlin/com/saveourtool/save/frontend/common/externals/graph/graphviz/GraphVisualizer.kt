@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")
@file:JsModule("react-graph-viz-engine")
@file:JsNonModule

package com.saveourtool.save.frontend.common.externals.graph.graphviz

import react.FC
import react.Props
import web.cssom.Height
import web.cssom.Width

@JsName("default")
external val graphVisualization: FC<GraphVisualizationProps>

/**
 * [Props] for [graphVisualization]
 *
 * To fetch data from a GraphQL endpoint, the component expects the following arguments:
 *
 * * graphqlUrl: the URL of the GraphQL endpoint
 * * graphqlQuery: the GraphQL query to execute
 *
 * Example:
 * ```JSON
 * graphqlUrl: "https://movies.neo4j-graphql.com/",
 * graphqlQuery:
 *     {
 *         actors(options: {limit: 20}) {
 *         __typename
 *         name
 *         acted_in {
 *             __typename
 *             title
 *             genres {
 *             __typename
 *             name
 *             }
 *         }
 *     }
 * }
 * ```
 */
external interface GraphVisualizationProps : Props {
    /**
     * Renderer that should be used to render graph
     * @see [GraphVizRenderer]
     */
    var renderer: String

    /**
     * The data format expected by the component is a JSON object and is based on the GraphQL response format sent by the Neo4j GraphQL library.
     *
     * Example:
     *
     * ```JSON
     * {
     *    "data": {
     *       "actors": [
     *          {
     *             "__typename": "Actor",
     *             "ID": 1,
     *             "name": "François Lallement",
     *             "acted_in": [
     *                ...nested list of objects
     *             ]
     *          }
     *       ]
     *    }
     * }
     * ```
     *
     * @see <a href=https://github.com/neo4j-field/react-graph-viz-engine/blob/main/src/stories/Graph.mdx#simple-data>docs</a>
     * @see <a href=https://github.com/neo4j-field/react-graph-viz-engine/blob/main/src/stories/Graph.stories.tsx>examples</a>
     */
    var data: dynamic

    /**
     * Layout that should be applied to graph
     * @see [GraphVizLayout]
     */
    var layout: String

    /**
     * [Width] of [graphVisualization]
     */
    var width: Width

    /**
     * [Height] of [graphVisualization]
     */
    var height: Height

    /**
     * Styles that can be applied to graph
     *
     * @see <a href=https://github.com/neo4j-field/react-graph-viz-engine/blob/main/src/stories/Graph.mdx#basic-styling>docs</a>
     */
    var style: GraphVizNodeStyle

    /**
     * Interactions that can be applied to graph
     *
     * @see <a href=https://github.com/neo4j-field/react-graph-viz-engine/blob/main/src/stories/Graph.mdx#interactions> docs </a>
     */
    var interactions: GraphVizInteractions

    /**
     * Schema for graph
     *
     * To override the automatic schema extraction logic, it is possible to manually pass label/id definitions to the component.
     *
     * @see <a href=https://github.com/neo4j-field/react-graph-viz-engine/blob/main/src/stories/Graph.mdx#custom-schema>docs</a>
     */
    var schema: dynamic

    /**
     * Flag to show navigator
     */
    var showNavigator: Boolean

    /**
     * Url of GraphQL (if required)
     */
    var graphqlUrl: String

    /**
     * Query of GraphQL (if required)
     */
    var graphqlQuery: String
}
