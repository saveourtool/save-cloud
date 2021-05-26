/**
 * A view with project details
 */

package org.cqfn.save.frontend.components.views

import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.Project
import org.cqfn.save.frontend.components.basic.cardComponent
import org.cqfn.save.frontend.externals.modal.modal
import org.cqfn.save.repository.GitRepository

import org.w3c.dom.HTMLInputElement
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.child
import react.dom.a
import react.dom.button
import react.dom.div
import react.dom.h1
import react.dom.h2
import react.dom.h6
import react.dom.input
import react.dom.p
import react.setState

import kotlinx.browser.window
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction

/**
 * [RProps] for project view
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface ProjectProps : RProps {
    var executionRequest: ExecutionRequest
}

/**
 * [RProps] retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface ProjectExecutionRouteProps : RProps {
    var name: String
    var owner: String
    var type: String
    var url: String
}

/**
 * [RState] of project view component
 */
external interface ProjectViewState : RState {
    /**
     * Whether error modal from backend is visible
     */
    var isErrorOpen: Boolean

    /**
     * Whether error text from backend is visible
     */
    var errorText: String
}

/**
 * A functional RComponent for project view
 *
 * @return a functional component
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
class ProjectView : RComponent<ProjectProps, ProjectViewState>() {
    private var pathToProperty: String? = null

    init {
        state.isErrorOpen = false
        state.errorText = ""
    }

    private fun submitExecutionRequest() {
        val executionRequest = ExecutionRequest(
            project = props.executionRequest.project,
            gitRepository = props.executionRequest.gitRepository,
            propertiesRelativePath = pathToProperty ?: "save.properties"
        )
        val jsonExecution = JSON.stringify(executionRequest)
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        window.fetch("http://localhost:5000/submitExecutionRequest", RequestInit(
            method = "POST",
            body = jsonExecution,
            headers = headers
        )).then {
            if (it.ok) {
                // fixme redirect to tests info
                window.location.href = "${window.location.origin}/history"
            } else {
                setState {
                    isErrorOpen = true
                    errorText = it.statusText
                }
            }
        }
    }

    @Suppress("TOO_LONG_FUNCTION")
    override fun RBuilder.render() {
        // modal window for configuring tests run - initially hidden
        runErrorModel()
        // Page Heading
        div("d-sm-flex align-items-center justify-content-between mb-4") {
            h1("h3 mb-0 text-gray-800") {
                +"Project ${props.executionRequest.project.name}"
            }
        }

        div("row") {
            child(cardComponent {
                div("text-center") {
                    div("pb-4") {
                        h6(classes = "d-inline") {
                            +"Path to property file: "
                        }
                        input(type = InputType.text, name = "itemText") {
                            attrs {
                                placeholder = "save.properties"
                                onChangeFunction = {
                                    val target = it.target as HTMLInputElement
                                    pathToProperty = target.value
                                }
                            }
                        }
                    }
                    button(type = ButtonType.button, classes = "btn btn-primary") {
                        attrs.onClickFunction = { submitExecutionRequest() }
                        +"Run tests now"
                    }
                }
            }) {
                attrs {
                    header = "Run tests"
                    leftBorderColor = "primary"
                }
            }

            child(cardComponent {
                p("small") {
                    +"Name: ${props.executionRequest.project.name}"
                }
                p("small") {
                    +"Description: ${props.executionRequest.project.description}"
                }
                p("small") {
                    a(href = "#/${props.executionRequest.project.owner}/${props.project.name}/history/latest") {
                        +"Latest test execution: N/A"
                    }
                }
                p("small") {
                    a(href = "#/${props.executionRequest.project.owner}/${props.project.name}/history") {
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

    private fun RBuilder.runErrorModel() = modal {
        attrs {
            isOpen = state.isErrorOpen
            contentLabel = "Error from backend"
        }
        div {
            h2("h3 mb-0 text-gray-800") {
                +state.errorText
            }
        }
        button(type = ButtonType.button, classes = "btn btn-primary") {
            attrs.onClickFunction = { setState { isErrorOpen = false } }
            +"Close"
        }
    }
}

/**
 * @return a [Project] constructed from these props
 */
@Suppress("EXTENSION_FUNCTION_WITH_CLASS")
fun ProjectExecutionRouteProps.toProject() = Project(
    owner = owner,
    name = name,
    description = "Todo: fetch description",
    url = "Todo: fetch URL",
)

/**
 * @return git repository
 */
@Suppress("EXTENSION_FUNCTION_WITH_CLASS")
fun ProjectExecutionRouteProps.toGitRepository() = GitRepository(url = "url")

/**
 * @return execution request
 */
@Suppress("EXTENSION_FUNCTION_WITH_CLASS")
fun ProjectExecutionRouteProps.toExecutionRequest() = ExecutionRequest(this.toProject(), this.toGitRepository())
