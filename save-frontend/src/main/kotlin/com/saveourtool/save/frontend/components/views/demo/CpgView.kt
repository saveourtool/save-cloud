/**
 * View for cpg
 */

package com.saveourtool.save.frontend.components.views.demo

import com.saveourtool.save.demo.cpg.CpgGraph
import com.saveourtool.save.demo.cpg.CpgResult
import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.components.basic.demoComponent
import com.saveourtool.save.frontend.components.modal.displaySimpleModal
import com.saveourtool.save.frontend.externals.sigma.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.loadingHandler

import csstype.ClassName
import csstype.Display
import csstype.Height
import js.core.jso
import react.VFC
import react.dom.html.ReactHTML.div
import react.useEffect
import react.useState

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val backgroundCard = cardComponent(hasBg = false, isPaddingBottomNull = true)

@Suppress("COMPLEX_EXPRESSION")
val cpgView: VFC = VFC {
    kotlinext.js.require("@react-sigma/core/lib/react-sigma.min.css")
    val cpgResultInit = CpgResult(CpgGraph.placeholder, "", emptyList())
    val (cpgResult, setCpgResult) = useState(cpgResultInit)
    val (isLogs, setIsLogs) = useState(false)
    val graphLoader = VFC {
        val loadGraph = useLoadGraph()
        val (_, assign) = useLayoutCircular()
        useEffect(assign, loadGraph) {
            loadGraph(cpgResult.cpgGraph.removeMultiEdges().paintNodes().toJson())
            assign()
        }
    }
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
                                    graphLoader()
                                }
                            }
                            div {
                                val alertStyle = if (cpgResult.applicationName.isNotBlank()) "alert-primary" else ""
                                className = ClassName("alert $alertStyle text-sm mt-3 pb-2 pt-2 mb-0")
                                +cpgResult.applicationName
                            }
                            if (isLogs && cpgResult.logs.isNotEmpty()) {
                                div {
                                    className = ClassName("alert alert-primary text-sm mt-3 pb-2 pt-2 mb-0")
                                    +(cpgResult.logs.joinToString("\n"))
                                }
                            }
                        }
                    }
                    this.showLogs = { setIsLogs(it) }
                }
            }
        }
    }
}
