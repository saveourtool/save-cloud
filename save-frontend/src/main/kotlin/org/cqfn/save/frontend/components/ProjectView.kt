/**
 * A view with project details
 */

package org.cqfn.save.frontend.components

import org.cqfn.save.frontend.components.basic.cardComponent

import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.child
import react.dom.a
import react.dom.button
import react.dom.div
import react.dom.h1
import react.dom.p

import kotlinx.html.ButtonType

/**
 * [RProps] for project view
 *
 * @property type type of project (github/binary/etc)
 * @property name name of the project
 * @property owner owner of the project (user of organization)
 * @property description project description
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface ProjectProps : RProps {
    var type: String  // todo type in common
    var name: String
    var owner: String
    var description: String
}

/**
 * A [RComponent] for project view
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
class ProjectView : RComponent<ProjectProps, RState>() {
    @Suppress("TOO_LONG_FUNCTION")
    override fun RBuilder.render() {
        // Page Heading
        div("d-sm-flex align-items-center justify-content-between mb-4") {
            h1("h3 mb-0 text-gray-800") {
                +"Project ${props.name}"
            }
        }

        div("row") {
            child(cardComponent {
                button(type = ButtonType.button, classes = "btn") {
                    +"Run tests now"
                }
            }) {
                attrs {
                    header = "Run tests"
                }
            }

            child(cardComponent {
                p("small") {
                    +"Name: ${props.name}"
                }
                p("small") {
                    +"Description: ${props.description}"
                }
                p("small") {
                    // todo: use router props?
                    // todo: links to individual history entires
                    a(href = "/${props.type}/${props.owner}/${props.name}/history/latest") {
                        +"Latest test execution: N/A"
                    }
                }
                p("small") {
                    a(href = "/${props.type}/${props.owner}/${props.name}/history") {
                        +"Execution history"
                    }
                }
            }) {
                attrs {
                    header = "Project info"
                }
            }
        }
    }
}
