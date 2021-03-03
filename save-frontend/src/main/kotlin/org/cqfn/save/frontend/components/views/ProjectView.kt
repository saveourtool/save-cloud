/**
 * A view with project details
 */

package org.cqfn.save.frontend.components.views

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
import org.cqfn.save.entities.Project

/**
 * [RProps] for project view
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface ProjectProps : RProps {
    var project: Project
}

external interface ProjectRouteProps : RProps {
    var name: String
    var owner: String
    var type: String
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
                +"Project ${props.project.name}"
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
                    leftBorderColor = "primary"
                }
            }

            child(cardComponent {
                p("small") {
                    +"Name: ${props.project.name}"
                }
                p("small") {
                    +"Description: ${props.project.description}"
                }
                p("small") {
                    // todo: use router props?
                    // todo: links to individual history entires
                    a(href = "#/${props.project.type}/${props.project.owner}/${props.project.name}/history/latest") {
                        +"Latest test execution: N/A"
                    }
                }
                p("small") {
                    a(href = "#/${props.project.type}/${props.project.owner}/${props.project.name}/history") {
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

fun ProjectRouteProps.toProject() = Project(
        owner = owner,
        name = name,
        type = type,
        description = "Todo: fetch description",
        url = "Todo: fetch URL"
    )
