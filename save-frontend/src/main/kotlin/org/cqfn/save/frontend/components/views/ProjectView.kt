/**
 * A view with project details
 */

package org.cqfn.save.frontend.components.views

import org.cqfn.save.entities.Project
import org.cqfn.save.frontend.components.basic.cardComponent
import org.cqfn.save.frontend.externals.modal.modal

import react.RProps
import react.child
import react.dom.a
import react.dom.button
import react.dom.div
import react.dom.h1
import react.dom.h2
import react.dom.li
import react.dom.p
import react.dom.ul

import kotlinx.html.ButtonType
import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.RComponent
import react.RState
import react.setState

/**
 * [RProps] for project view
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface ProjectProps : RProps {
    var project: Project
}

/**
 * [RProps] retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface ProjectRouteProps : RProps {
    var name: String
    var owner: String
    var type: String
}

/**
 * @return a [Project] constructed from these props
 */
@Suppress("EXTENSION_FUNCTION_WITH_CLASS")
fun ProjectRouteProps.toProject() = Project(
    owner = owner,
    name = name,
    type = type,
    description = "Todo: fetch description",
    url = "Todo: fetch URL",
)

external interface ProjectViewState : RState {
    var isModalOpen: Boolean
}

/**
 * A functional RComponent for project view
 *
 * @return a functional component
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
class ProjectView : RComponent<ProjectProps, ProjectViewState>() {
    init {
        state.isModalOpen = false
    }

    override fun RBuilder.render() {
        // Page Heading
        div("d-sm-flex align-items-center justify-content-between mb-4") {
            h1("h3 mb-0 text-gray-800") {
                +"Project ${props.project.name}"
            }
            modal {
                attrs {
                    isOpen = state.isModalOpen
                    contentLabel = "Submit tests execution"
                }
                div {
                    h2("h3 mb-0 text-gray-800") {
                        +"Choose type of test execution"
                    }
                    ul("nav nav-pills") {
                        li("nav-item") {
                            a(classes = "nav-link") {
                                +"Repo URL"
                            }
                        }
                        li("nav-item") {
                            a(classes = "nav-link") {
                                +"Upload an executable"
                            }
                        }
                    }
                }
                button(type = ButtonType.button, classes = "btn btn-primary") {
                    attrs.onClickFunction = { setState { isModalOpen = false } }
                    +"Close"
                }
            }
        }

        div("row") {
            child(cardComponent {
                button(type = ButtonType.button, classes = "btn btn-primary") {
                    attrs.onClickFunction = { setState { isModalOpen = true } }
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
