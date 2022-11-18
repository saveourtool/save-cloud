package com.saveourtool.save.frontend.components.views.demo

import com.saveourtool.save.demo.cpg.*
import com.saveourtool.save.frontend.externals.sigma.*

import csstype.ClassName
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.label

import kotlinx.js.jso

val cpgDemoView = cpgDemoView()

private val graphLoader = VFC {
    val loadGraph = useLoadGraph()
    val (_, assign) = useLayoutRandom()
    useEffect(assign, loadGraph) {
        loadGraph(getGraph())
        assign()
    }
}

private fun getGraph() = CpgGraph.placeholder.toJson()

private fun cpgDemoView(): VFC = VFC {
    kotlinext.js.require("@react-sigma/core/lib/react-sigma.min.css")
    div {
        className = ClassName("d-flex justify-content-center")
        div {
            className = ClassName("col-8")
            div {
                label {
                    className = ClassName("text-center")
                    +"Sigma test"
                }
                sigmaContainer {
                    style = jso {
                        height = "1000px"
                        width = "1000px"
                    }
                    settings = getSigmaContainerSettings()
                    graphLoader()
                }
            }
        }
    }
}
