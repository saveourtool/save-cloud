/**
 * View for cpg
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.demo

import com.saveourtool.save.demo.cpg.CpgResult
import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.components.basic.cpg.graphEvents
import com.saveourtool.save.frontend.components.basic.cpg.graphLoader
import com.saveourtool.save.frontend.components.basic.demoComponent
import com.saveourtool.save.frontend.components.modal.displaySimpleModal
import com.saveourtool.save.frontend.externals.sigma.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.loadingHandler
import com.saveourtool.save.utils.Languages

import csstype.ClassName
import csstype.Display
import csstype.Height
import js.core.jso
import react.*
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.div

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

private val backgroundCard = cardComponent(hasBg = false, isPaddingBottomNull = true)

@Suppress(
    "EMPTY_BLOCK_STRUCTURE_ERROR",
)
val cpgView: VFC = VFC {
    kotlinext.js.require("@react-sigma/core/lib/react-sigma.min.css")
    val (cpgResult, setCpgResult) = useState(CpgResult.empty)
    val (isLogs, setIsLogs) = useState(false)

    val (errorMessage, setErrorMessage) = useState("")
    val errorWindowOpenness = useWindowOpenness()

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
                                className = ClassName("card card-body")
                                style = jso {
                                    height = "83%".unsafeCast<Height>()
                                    display = Display.block
                                }
                                val graphology = kotlinext.js.require("graphology")
                                sigmaContainer {
                                    settings = getSigmaContainerSettings()
                                    this.graph = graphology.MultiDirectedGraph
                                    graphEvents()
                                    graphLoader {
                                        cpgGraph = cpgResult.cpgGraph
                                    }
                                }
                            }
                            div {
                                val alertStyle = if (cpgResult.applicationName.isNotBlank()) "alert-primary" else ""
                                className = ClassName("alert $alertStyle text-sm mt-3 pb-2 pt-2 mb-0")
                                +cpgResult.applicationName
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
                    +log
                    br { }
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
