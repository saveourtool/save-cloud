/**
 * A view with project creation details
 */

package org.cqfn.save.frontend.components.views

import org.cqfn.save.entities.GitDto
import org.cqfn.save.entities.NewProjectDto
import org.cqfn.save.entities.Project
import org.cqfn.save.frontend.utils.post
import org.cqfn.save.frontend.utils.runErrorModal

import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.Event
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import react.PropsWithChildren
import react.RBuilder
import react.RComponent
import react.State
import react.dom.attrs
import react.dom.button
import react.dom.div
import react.dom.form
import react.dom.h1
import react.dom.hr
import react.dom.input
import react.dom.textarea
import react.setState

import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * [RState] of project creation view component
 */
external interface ProjectSaveViewState : State {
    /**
     * Flag to handle error
     */
    var isErrorWithProjectSave: Boolean?

    /**
     * Error message
     */
    var errorMessage: String
}

/**
 * A functional RComponent for project creation view
 *
 * @return a functional component
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class CreationView : RComponent<PropsWithChildren, ProjectSaveViewState>() {
    private val projectFieldsMap: MutableMap<String, String> = mutableMapOf()
    private val gitFieldsMap: MutableMap<String, String> = mutableMapOf()
    private lateinit var responseFromCreationProject: Response

    init {
        state.isErrorWithProjectSave = false
        state.errorMessage = ""
    }

    private fun changeFields(fieldName: String, target: Event, isProject: Boolean = true) {
        val tg = target.target as HTMLInputElement
        if (isProject) projectFieldsMap[fieldName] = tg.value else gitFieldsMap[fieldName] = tg.value
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun saveProject() {
        val newProjectRequest = NewProjectDto(
            Project(projectFieldsMap["owner"]!!, projectFieldsMap["name"]!!, projectFieldsMap["url"], projectFieldsMap["description"]),
            gitFieldsMap["url"]?.let { GitDto(it, gitFieldsMap["username"], gitFieldsMap["password"], gitFieldsMap["branch"]) }
        )
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        GlobalScope.launch {
            responseFromCreationProject = post("${window.location.origin}/saveProject", headers, Json.encodeToString(newProjectRequest))
        }.invokeOnCompletion {
            if (responseFromCreationProject.ok) {
                window.location.href = "${window.location.origin}#/${newProjectRequest.project.owner}/${newProjectRequest.project.name}"
            } else {
                responseFromCreationProject.text().then {
                    setState {
                        isErrorWithProjectSave = true
                        errorMessage = it
                    }
                }
            }
        }
    }

    @Suppress("TOO_LONG_FUNCTION", "EMPTY_BLOCK_STRUCTURE_ERROR", "LongMethod")
    override fun RBuilder.render() {
        runErrorModal(
            state.isErrorWithProjectSave,
            "Error with project creation",
            state.errorMessage
        ) {
            setState { isErrorWithProjectSave = false }
        }
        div("container card o-hidden border-0 shadow-lg my-5 card-body p-0") {
            div("p-5 text-center") {
                h1("h4 text-gray-900 mb-4") {
                    +"Add a new project"
                }
                form(classes = "user") {
                    div("form-group row") {
                        div("col-sm-6 mb-3 mb-sm-0") {
                            input(type = InputType.text, classes = "form-control form-control-user") {
                                attrs {
                                    required = true
                                    placeholder = "Name"
                                    onChangeFunction = {
                                        changeFields("name", it)
                                    }
                                }
                            }
                        }
                        div("col-sm-6") {
                            input(type = InputType.text, classes = "form-control form-control-user") {
                                attrs {
                                    required = true
                                    placeholder = "Owner"
                                    onChangeFunction = {
                                        changeFields("owner", it)
                                    }
                                }
                            }
                        }
                    }
                    div("form-group row") {
                        input(type = InputType.text, classes = "form-control form-control-user") {
                            attrs {
                                placeholder = "URL"
                                onChangeFunction = {
                                    changeFields("URL", it)
                                }
                            }
                        }
                    }
                    div("form-group row") {
                        textarea(classes = "form-control form-control-user") {
                            attrs {
                                placeholder = "Description"
                                onChangeFunction = {
                                    val tg = it.target as HTMLTextAreaElement
                                    projectFieldsMap["description"] = tg.value
                                }
                            }
                        }
                    }
                    hr {}
                    div("form-group row") {
                        input(type = InputType.text, classes = "form-control form-control-user") {
                            attrs {
                                placeholder = "Git url"
                                onChangeFunction = {
                                    changeFields("url", it, false)
                                }
                            }
                        }
                    }
                    div("form-group row") {
                        div("col-sm-6 mb-3 mb-sm-0") {
                            input(type = InputType.text, classes = "form-control form-control-user") {
                                attrs {
                                    placeholder = "Git username"
                                    onChangeFunction = {
                                        changeFields("username", it, false)
                                    }
                                }
                            }
                        }
                        div("col-sm-6") {
                            input(type = InputType.password, classes = "form-control form-control-user") {
                                attrs {
                                    placeholder = "Git password"
                                    onChangeFunction = {
                                        changeFields("password", it, false)
                                    }
                                }
                            }
                        }
                    }
                    button(type = ButtonType.button, classes = "btn btn-primary") {
                        +"Create"
                        attrs.onClickFunction = { saveProject() }
                    }
                }
            }
        }
    }
}
