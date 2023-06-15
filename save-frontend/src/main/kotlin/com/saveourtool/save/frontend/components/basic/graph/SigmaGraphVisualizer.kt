@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.graph

import com.saveourtool.save.demo.cpg.CpgGraph
import com.saveourtool.save.demo.cpg.CpgNodeAdditionalInfo
import com.saveourtool.save.frontend.components.basic.cpg.SigmaLayout
import com.saveourtool.save.frontend.components.basic.cpg.graphEvents
import com.saveourtool.save.frontend.components.basic.cpg.graphLoader
import com.saveourtool.save.frontend.externals.fontawesome.faTimesCircle
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.externals.graph.getSigmaContainerSettings
import com.saveourtool.save.frontend.externals.graph.sigma.sigmaContainer
import js.core.jso
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.pre
import react.dom.html.ReactHTML.small
import react.dom.html.ReactHTML.table
import react.dom.html.ReactHTML.tbody
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.thead
import react.dom.html.ReactHTML.tr
import react.useState
import web.cssom.*
import web.html.ButtonType

private const val NOT_PROVIDED = "NOT_PROVIDED"

/**
 * Graph visualizer based on Sigma and Graphology libs
 */
val sigmaGraphVisualizer: FC<SigmaGraphVisualizerProps> = FC { props ->
    kotlinext.js.require("@react-sigma/core/lib/react-sigma.min.css")
    val (selectedNodeName, setSelectedNodeName) = useState<String?>(null)
    val graphology = kotlinext.js.require("graphology")
    sigmaContainer {
        settings = getSigmaContainerSettings()
        this.graph = graphology.MultiDirectedGraph
        graphEvents {
            shouldHideUnfocusedNodes = true
            setSelectedNode = { newSelectedNodeName ->
                setSelectedNodeName { previousSelectedNodeName ->
                    newSelectedNodeName.takeIf { it != previousSelectedNodeName }
                }
            }
        }
        graphLoader {
            this.cpgGraph = props.cpgGraph
            this.selectedLayout = props.layout
        }
    }
    div {
        id = "collapse"
        val show = selectedNodeName?.let { nodeName ->
            props.cpgGraph
                .nodes
                .find { node -> node.key == nodeName }
                ?.let { node ->
                    displayCpgNodeAdditionalInfo(
                        node.attributes.label,
                        props.query,
                        node.attributes.additionalInfo,
                    ) {
                        setSelectedNodeName(it)
                    }
                }
            "show"
        } ?: "hide"
        className = ClassName("col-6 p-0 position-absolute width overflow-auto $show")
        style = jso {
            top = "0px".unsafeCast<Top>()
            right = "0px".unsafeCast<Right>()
            maxHeight = "100%".unsafeCast<MaxHeight>()
        }
    }
}

@Suppress("TYPE_ALIAS")
private val additionalInfoMapping: Map<String, (String, CpgNodeAdditionalInfo?) -> String?> = mapOf(
    "Code" to { _, info -> info?.code },
    "File" to { applicationName, info -> info?.file?.formatPathToFile(applicationName, "demo") },
    "Comment" to { _, info -> info?.comment },
    "Argument index" to { _, info -> info?.argumentIndex?.toString() },
    "isImplicit" to { _, info -> info?.isImplicit?.toString() },
    "isInferred" to { _, info -> info?.isInferred?.toString() },
    "Location" to { _, info -> info?.location },
)

/**
 * [Props] for [sigmaGraphVisualizer]
 */
external interface SigmaGraphVisualizerProps : Props {
    /**
     * Graph to render
     */
    var cpgGraph: CpgGraph

    /**
     * Query to neo4J
     */
    var query: String

    /**
     * [SigmaLayout] to apply to graph
     */
    var layout: SigmaLayout
}

private fun ChildrenBuilder.displayCpgNodeAdditionalInfo(
    nodeName: String?,
    applicationName: String,
    additionalInfo: CpgNodeAdditionalInfo?,
    setSelectedNodeName: (String?) -> Unit,
) {
    button {
        className = ClassName("btn p-0 position-absolute")
        fontAwesomeIcon(faTimesCircle)
        type = "button".unsafeCast<ButtonType>()
        onClick = { setSelectedNodeName(null) }
        style = jso {
            top = "0%".unsafeCast<Top>()
            right = "1%".unsafeCast<Right>()
        }
    }
    table {
        thead {
            tr {
                className = ClassName("bg-dark text-light")
                th {
                    scope = "col"
                    +"Name"
                }
                th {
                    scope = "col"
                    +(nodeName ?: NOT_PROVIDED).formatPathToFile(applicationName)
                }
            }
        }
        tbody {
            additionalInfoMapping.map { (label, valueGetter) ->
                label to (valueGetter(applicationName, additionalInfo) ?: NOT_PROVIDED)
            }.forEachIndexed { index, (label, value) ->
                tr {
                    if (index % 2 == 1) {
                        className = ClassName("bg-light")
                    }
                    td {
                        small {
                            +label
                        }
                    }
                    td {
                        pre {
                            className = ClassName("m-0")
                            style = jso {
                                fontSize = FontSize.small
                            }
                            +value
                        }
                    }
                }
            }
        }
    }
}

private fun String.formatPathToFile(
    applicationName: String,
    missingDelimiterValue: String? = null,
) = missingDelimiterValue?.let {
    substringAfterLast("$applicationName/", missingDelimiterValue)
} ?: substringAfterLast("$applicationName/")
