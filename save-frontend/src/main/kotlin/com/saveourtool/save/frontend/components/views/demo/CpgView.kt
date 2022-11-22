/**
 * View for cpg
 */

package com.saveourtool.save.frontend.components.views.demo

import com.saveourtool.save.demo.cpg.CpgGraph
import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.components.basic.demoComponent
import com.saveourtool.save.frontend.externals.sigma.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.loadingHandler
import com.saveourtool.save.frontend.utils.responseHandlerWithValidation

import csstype.ClassName
import react.VFC
import react.dom.html.ReactHTML.div
import react.useEffect
import react.useState

import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val backgroundCard = cardComponent(hasBg = false, isPaddingBottomNull = true)

val cpgView: VFC = VFC {
    kotlinext.js.require("@react-sigma/core/lib/react-sigma.min.css")
    val (graph, setGraph) = useState(CpgGraph.placeholder)
    val graphLoader = VFC {
        val loadGraph = useLoadGraph()
        val (_, assign) = useLayoutRandom()
        useEffect(assign, loadGraph) {
            loadGraph(graph.paintNodes().toJson())
            assign()
        }
    }
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
                            responseHandler = ::responseHandlerWithValidation
                        )

                        if (response.ok) {
                            val newGraph: CpgGraph = response.decodeFromJsonString()
                            setGraph(newGraph)
                        } else {
                            window.alert(response.unpackMessage())
                        }
                    }
                    this.resultBuilder = { builder ->
                        with(builder) {
                            sigmaContainer {
                                settings = getSigmaContainerSettings()
                                graphLoader()
                            }
                        }
                    }
                }
            }
        }
    }
}
