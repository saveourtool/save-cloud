package org.cqfn.save.frontend.components

import org.cqfn.save.frontend.components.basic.cardComponent
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.child
import react.dom.br
import react.dom.button
import react.dom.div
import react.dom.h1
import react.dom.p
import react.router.dom.RouteResultProps

external interface ProjectProps : RProps {
    var type: String  // todo type in common
    var name: String
    var owner: String
    var description: String
}

@OptIn(ExperimentalJsExport::class)
@JsExport
class ProjectView : RComponent<RouteResultProps<ProjectProps>, RState>() {
    override fun RBuilder.render() {
        // Page Heading
        div("d-sm-flex align-items-center justify-content-between mb-4") {
            h1("h3 mb-0 text-gray-800") {
                +"Project ${props.match.params.name}"
            }
        }

        div("row") {
            child(cardComponent() {
                button {
                    +"Run tests now"
                }
            }) {
                attrs {
                    header = "Run tests"
                }
            }

            child(cardComponent() {
                p {
                    +"Name: ${props.match.params.name}"
                }
                br {}
                p {
                    +"Description: ${props.match.params.description}"
                }
                br {}
                p {
                    +"Latest test execution: N/A"
                }
                br {}
            }) {
                attrs {
                    header = "Project info"
                }
            }
        }
    }
}
