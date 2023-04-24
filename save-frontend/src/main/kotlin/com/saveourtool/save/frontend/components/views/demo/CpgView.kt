/**
 * View for cpg
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.demo

import com.saveourtool.save.demo.cpg.CpgNodeAdditionalInfo
import com.saveourtool.save.demo.cpg.CpgResult
import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.components.basic.cpg.SigmaLayout
import com.saveourtool.save.frontend.components.basic.cpg.graphEvents
import com.saveourtool.save.frontend.components.basic.cpg.graphLoader
import com.saveourtool.save.frontend.components.basic.demoComponent
import com.saveourtool.save.frontend.components.modal.displaySimpleModal
import com.saveourtool.save.frontend.externals.fontawesome.faTimesCircle
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.externals.sigma.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.loadingHandler
import com.saveourtool.save.utils.Languages

import js.core.jso
import react.*
import react.dom.html.ReactHTML.br
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
import web.cssom.*
import web.html.ButtonType

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val CPG_PLACEHOLDER_TEXT = """#include <iostream>

int main() {
    int a;
    std::cin >> a;
    std::cout << (a + a) << std::endl;
    return 0;    
}
"""

private const val NOT_PROVIDED = "NOT_PROVIDED"

private val backgroundCard = cardComponent(hasBg = false, isPaddingBottomNull = true)

@Suppress(
    "EMPTY_BLOCK_STRUCTURE_ERROR",
)
val cpgView: VFC = VFC {
    kotlinext.js.require("@react-sigma/core/lib/react-sigma.min.css")
    useBackground(Style.WHITE)
    val (cpgResult, setCpgResult) = useState(CpgResult.empty)
    val (isLogs, setIsLogs) = useState(false)

    val (errorMessage, setErrorMessage) = useState("")
    val errorWindowOpenness = useWindowOpenness()

    val (selectedNodeName, setSelectedNodeName) = useState<String?>(null)

    val (selectedLayout, setSelectedLayout) = useState(SigmaLayout.preferredLayout)

    displaySimpleModal(
        errorWindowOpenness,
        "Error log",
        errorMessage,
    )

    div {
        className = ClassName("d-flex justify-content-center mb-2")
        div {
            className = ClassName("col-12")
            backgroundCard {
                demoComponent {
                    this.selectedLayout = selectedLayout
                    this.setSelectedLayout = { setSelectedLayout(it) }
                    this.placeholderText = CPG_PLACEHOLDER_TEXT
                    this.preselectedLanguage = Languages.CPP
                    this.resultRequest = { demoRequest ->
                        val response = post(
                            "$cpgDemoApiUrl/upload-code",
                            headers = jsonHeaders,
                            body = Json.encodeToString(demoRequest),
                            loadingHandler = ::loadingHandler,
                        )

                        if (response.ok) {
                            val cpgResultNew: CpgResult = response.unsafeMap {
                                it.decodeFromJsonString()
                            }
                            setCpgResult(cpgResultNew)
                            setIsLogs(false)
                        } else {
                            setErrorMessage(response.unpackMessage())
                            errorWindowOpenness.openWindow()
                        }
                    }
                    this.resultBuilder = { builder ->
                        with(builder) {
                            div {
                                className = ClassName("card card-body px-0 py-0")
                                style = jso {
                                    height = "83%".unsafeCast<Height>()
                                    display = Display.block
                                }
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
                                        this.cpgGraph = cpgResult.cpgGraph
                                        this.selectedLayout = selectedLayout
                                    }
                                }
                                div {
                                    id = "collapse"
                                    val show = selectedNodeName?.let { nodeName ->
                                        cpgResult.cpgGraph.nodes.find { node -> node.key == nodeName }?.let { node ->
                                            displayCpgNodeAdditionalInfo(
                                                node.attributes.label,
                                                cpgResult.query,
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
                            div {
                                val alertStyle = when {
                                    cpgResult.query.isBlank() -> ""
                                    cpgResult.query.startsWith("Error") -> "alert-warning"
                                    else -> "alert-primary"
                                }
                                className = ClassName("alert $alertStyle text-sm mt-3 pb-2 pt-2 mb-0")
                                +cpgResult.query
                            }
                        }
                    }
                    this.changeLogsVisibility = {
                        setIsLogs { !it }
                    }
                }
            }
        }
    }
    if (isLogs) {
        div {
            val alertStyle = if (cpgResult.logs.isNotEmpty()) {
                cpgResult.logs.forEach { log ->
                    when {
                        log.contains("ERROR") || log.startsWith("Exception:") -> pre {
                            className = ClassName("text-danger")
                            +log
                        }
                        else -> {
                            +log
                            br { }
                        }
                    }
                }

                "alert-primary"
            } else {
                +"No logs provided"

                "alert-secondary"
            }
            className = ClassName("alert $alertStyle text-sm mt-3 pb-2 pt-2 mb-0")
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

@Suppress("TOO_LONG_FUNCTION", "LongMethod")
private fun ChildrenBuilder.displayCpgNodeAdditionalInfo(
    nodeName: String?,
    applicationName: String,
    additionalInfo: CpgNodeAdditionalInfo?,
    setSelectedNodeName: (String?) -> Unit,
) {
    div {
        className = ClassName("card card-body p-0")
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
                }
                    .forEachIndexed { index, (label, value) ->
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
}

private fun String.formatPathToFile(
    applicationName: String,
    missingDelimiterValue: String? = null,
) = missingDelimiterValue?.let {
    substringAfterLast("$applicationName/", missingDelimiterValue)
} ?: substringAfterLast("$applicationName/")
