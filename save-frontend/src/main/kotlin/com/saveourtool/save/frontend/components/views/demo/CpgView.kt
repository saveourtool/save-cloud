/**
 * View for cpg
 */

package com.saveourtool.save.frontend.components.views.demo

import com.saveourtool.save.demo.cpg.CpgGraph
import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.components.basic.cpgDemoComponent

import com.saveourtool.save.frontend.externals.sigma.*
import csstype.ClassName
import react.VFC
import react.dom.html.ReactHTML.div
import react.useEffect
import react.useState

private val backgroundCard = cardComponent(hasBg = false, isPaddingBottomNull = true)

val cpgView = cpgView()

private fun cpgView(): VFC = VFC {
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
                cpgDemoComponent {
                    this.builderModal = { builder ->
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
