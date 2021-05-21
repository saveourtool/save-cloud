/**
 * A view with project details
 */

package org.cqfn.save.frontend.components.views

import kotlinx.browser.window
import org.cqfn.save.entities.Project
import org.cqfn.save.frontend.components.basic.cardComponent
import org.cqfn.save.frontend.externals.modal.modal

import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.child
import react.setState

import kotlinx.html.ButtonType
import kotlinx.html.js.onClickFunction
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.repository.GitRepository
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit
import react.dom.*
import kotlin.js.Json
import kotlin.js.json

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
     * Whether modal for tests execution request submission is visible
     */
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

    fun submitExecutionRequest() {
        val jsonExecution = JSON.stringify(props.executionRequest)
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        window.fetch("http://localhost:5000/submitExecutionRequest", RequestInit(
            method = "POST",
            body = jsonExecution,
            headers = headers
        )).then { it.status }
        window.location.href = "http://localhost:8080/#/"
    }

    @Suppress("TOO_LONG_FUNCTION")
    override fun RBuilder.render() {
        // modal window for configuring tests run - initially hidden

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
                        input {
                            attrs.placeholder = "save.property"
                        }
                    }
                    button(type = ButtonType.button, classes = "btn btn-primary") {
                        attrs.onClickFunction = {
                            submitExecutionRequest()
                        }
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
                    a(href = "#/${props.executionRequest.project.type}/${props.executionRequest.project.owner}/${props.executionRequest.project.name}/history/latest") {
                        +"Latest test execution: N/A"
                    }
                }
                p("small") {
                    a(href = "#/${props.executionRequest.project.type}/${props.executionRequest.project.owner}/${props.executionRequest.project.name}/history") {
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

/**
 * @return a [Project] constructed from these props
 */
@Suppress("EXTENSION_FUNCTION_WITH_CLASS")
fun ProjectExecutionRouteProps.toProject() = Project(
      owner = owner,
      name = name,
      type = type,
      description = "Todo: fetch description",
      url = "Todo: fetch URL",
  )

fun ProjectExecutionRouteProps.toGitRepository() = GitRepository(url = "url")

fun ProjectExecutionRouteProps.toExecutionRequest() = ExecutionRequest(this.toProject(), this.toGitRepository())
