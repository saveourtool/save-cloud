@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.externals.graph.cytoscape

import com.saveourtool.save.demo.cpg.cytoscape.CytoscapeGraph
import com.saveourtool.save.demo.cpg.cytoscape.CytoscapeLayout

import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.div
import web.cssom.*
import web.dom.document

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val CYTOSCAPE_DIV_ID = "cytoscape-div"

/**
 * @param graph data in [CytoscapeGraph] format
 * @param layout [CytoscapeLayout] that should be applied to graph
 * @param divId id that should be assigned to div with graph, [CYTOSCAPE_DIV_ID] by default
 * @param selectionType type of node selection
 * @return cytoscape graph as dynamic
 */
@Suppress(
    "UNUSED_VARIABLE",
    "TOO_LONG_FUNCTION",
    "UNUSED_PARAMETER",
    "LongMethod"
)
fun ChildrenBuilder.cytoscape(
    graph: CytoscapeGraph,
    layout: CytoscapeLayout,
    divId: String = CYTOSCAPE_DIV_ID,
    selectionType: String = "single",
): dynamic {
    div {
        id = divId
        style = jso {
            width = "100%".unsafeCast<Width>()
            height = "90%".unsafeCast<Height>()
        }
    }

    val cytoscapeGraphJsonString = Json.encodeToString(graph)
    val cytoscapeGraphJsonStringJs = js("JSON.parse(cytoscapeGraphJsonString);")
    // language=json
    val graphStyle = js("""
        [
            {
                "selector": "node",
                "style": {
                    "background-color": "#666",
                    "label": "data(label)",
                    "height": 10,
                    "width": 10,
                    "font-size": 3
                }
            },
            {
                "selector": "edge",
                "style": {
                    "width": 2,
                    "height": 2,
                    "line-color": "#ccc",
                    "target-arrow-color": "#ccc",
                    "target-arrow-shape": "triangle",
                    "curve-style": "bezier",
                    "label": "data(label)",
                    "font-size": 3,
                    "arrow-scale": 0.5
                }
            }
        ]
    """)
    val layoutName = layout.layoutName
    val graphLayout = js("""
        {
            "name": layoutName,
            "padding": 5
        }
    """)
    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    val options = document.getElementById(divId)?.let { divContainer ->
        js("""
            {
                "container" : divContainer,
                "elements" : cytoscapeGraphJsonStringJs,
                "layout" : graphLayout,
                "style" : graphStyle,
                "selectionType": selectionType
            }
        """)
    }

    val cytoscapeJs = kotlinext.js.require("cytoscape")
    return cytoscapeJs(options)
}
