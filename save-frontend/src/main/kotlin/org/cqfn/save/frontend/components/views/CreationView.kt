file:@Suppress("WildcardImport")
/**
 * A view with project creation details
 */

package org.cqfn.save.frontend.components.views

import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.cqfn.save.frontend.utils.runErrorModal

import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.Event
import org.w3c.fetch.Response
import react.PropsWithChildren
import react.RBuilder
import react.RComponent
import react.State
import react.setState

import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.cqfn.save.entities.GitDto
import org.cqfn.save.entities.NewProjectDto
import org.cqfn.save.entities.Project
import org.cqfn.save.frontend.externals.fontawesome.faQuestionCircle
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon
import org.cqfn.save.frontend.utils.post
import org.w3c.fetch.Headers
import react.dom.*

internal enum class InputTypes(val str: String) {
    OWNER("owner name"),
    PROJECT_NAME("project name"),
    PROJECT_URL("project Url"),
    DESCRIPTION("project description"),
    GIT_URL("git Url"),
    GIT_USER("git username"),
    GIT_TOKEN("git token"),
    GIT_BRANCH("git branch")
}

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

    /**
     * Validation of input fields
     */
    var isValidOwner: Boolean?

    /**
     * Validation of input fields
     */
    var isValidProjectName: Boolean?

    /**
     * Validation of input fields
     */
    var isValidGitUrl: Boolean?

    /**
     * Validation of input fields
     */
    var isValidGitUser: Boolean?

    /**
     * Validation of input fields
     */
    var isValidGitToken: Boolean?
}

/**
 * A functional RComponent for project creation view
 *
 * @return a functional component
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class CreationView : RComponent<PropsWithChildren, ProjectSaveViewState>() {
    private val fieldsMap: MutableMap<InputTypes, String> = mutableMapOf()
    private lateinit var responseFromCreationProject: Response

    init {
        state.isErrorWithProjectSave = false
        state.errorMessage = ""

        state.isValidOwner = true
        state.isValidProjectName = true
        state.isValidGitUrl = true
        state.isValidGitUser = true
        state.isValidGitToken = true
    }

    private fun changeFields(fieldName: InputTypes, target: Event, isProject: Boolean = true) {
        val tg = target.target as HTMLInputElement
        if (isProject) fieldsMap[fieldName] = tg.value else fieldsMap[fieldName] = tg.value
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun saveProject(e: Event) {
        if (!isValidInput()) {
            return
        }
        val newProjectRequest = NewProjectDto(
            Project(
                fieldsMap[InputTypes.OWNER]!!.trim(),
                fieldsMap[InputTypes.PROJECT_NAME]!!.trim(),
                fieldsMap[InputTypes.PROJECT_URL]?.trim(),
                fieldsMap[InputTypes.DESCRIPTION]?.trim()
            ),
            GitDto(
                fieldsMap[InputTypes.GIT_URL]!!.trim(),
                fieldsMap[InputTypes.GIT_USER]!!.trim(),
                fieldsMap[InputTypes.GIT_TOKEN]!!.trim(),
                fieldsMap[InputTypes.GIT_BRANCH]?.trim()
            )
        )
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        GlobalScope.launch {
            responseFromCreationProject =
                post("${window.location.origin}/saveProject", headers, Json.encodeToString(newProjectRequest))
        }.invokeOnCompletion {
            if (responseFromCreationProject.ok) {
                window.location.href =
                    "${window.location.origin}#/${newProjectRequest.project.owner}/${newProjectRequest.project.name}"
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

    /**
     * A little bit ugly method with code duplication due to different states.
     * FixMe: May be it will be possible to optimize it in the future, now we don't have time.
     */
    private fun isValidInput(): Boolean {
        var valid = true
        if (fieldsMap[InputTypes.OWNER].isNullOrBlank()) {
            setState { isValidOwner = false }
            valid = false
        } else {
            setState { isValidOwner = true }
        }

        if (fieldsMap[InputTypes.PROJECT_NAME].isNullOrBlank()) {
            setState { isValidProjectName = false }
            valid = false
        } else {
            setState { isValidProjectName = true }
        }

        val gitUser = fieldsMap[InputTypes.GIT_USER]
        if (gitUser.isNullOrBlank() || gitUser.trim().matches(".*\\s.*")) {
            setState { isValidGitUser = false }
            valid = false
        } else {
            setState { isValidGitUser = true }
        }

        val gitToken = fieldsMap[InputTypes.GIT_TOKEN]
        if (gitToken.isNullOrBlank() || gitToken.trim().matches(".*\\s.*")) {
            setState { isValidGitToken = false }
            valid = false
        } else {
            setState { isValidGitToken = true }
        }

        val gitUrl = fieldsMap[InputTypes.GIT_URL]
        if (gitUrl.isNullOrBlank() || !gitUrl.trim().startsWith("http")) {
            setState { isValidGitUrl = false }
            valid = false
        } else {
            setState { isValidGitUrl = true }
        }
        return valid
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

        div("row justify-content-center") {
            div("col-sm-6") {
                div("container card o-hidden border-0 shadow-lg my-2 card-body p-0") {
                    div("p-5 text-center") {
                        h1("h4 text-gray-900 mb-4") {
                            +"Create new test project"
                        }
                        form(classes = "needs-validation") {
                            div("row g-3") {
                                inputTextFormRequired(InputTypes.OWNER, "col-md-6 pl-0", "Owner name") {
                                    changeFields(InputTypes.OWNER, it)
                                }
                                inputTextFormRequired(InputTypes.PROJECT_NAME, "col-md-6", "Tested tool name") {
                                    changeFields(InputTypes.PROJECT_NAME, it)
                                }
                                inputTextFormOptional(InputTypes.PROJECT_URL, "col-md-6 pr-0 mt-3", "Tested tool Url") {
                                    changeFields(InputTypes.PROJECT_URL, it)
                                }
                                inputTextFormRequired(
                                    InputTypes.GIT_URL,
                                    "col-md-6 mt-3 pl-0",
                                    "Test repository Git Url"
                                ) {
                                    changeFields(InputTypes.GIT_URL, it, false)
                                }
                                inputTextFormRequired(InputTypes.GIT_USER, "col-md-6 mt-3", "Git Username") {
                                    changeFields(InputTypes.GIT_USER, it, false)
                                }
                                inputTextFormRequired(InputTypes.GIT_TOKEN, "col-md-6 mt-3 pr-0", "Git Token") {
                                    changeFields(InputTypes.GIT_TOKEN, it, false)
                                }
                                div("col-md-12 mt-3 mb-3 pl-0 pr-0") {
                                    label("form-label") {
                                        attrs.set("for", InputTypes.DESCRIPTION.name)
                                        +"Description"
                                    }
                                    div("input-group has-validation") {
                                        span("input-group-text") {
                                            attrs["id"] = "${InputTypes.DESCRIPTION.name}Span"
                                            +"Optional"
                                        }
                                        textarea("form-control") {
                                            attrs {
                                                onChangeFunction = {
                                                    val tg = it.target as HTMLTextAreaElement
                                                    fieldsMap[InputTypes.DESCRIPTION] = tg.value
                                                }
                                            }
                                            attrs["aria-describedby"] = "${InputTypes.DESCRIPTION.name}Span"
                                            attrs["row"] = "2"
                                            attrs["id"] = InputTypes.DESCRIPTION.name
                                            attrs["required"] = false
                                            attrs["class"] = "form-control"
                                        }
                                    }
                                }

                            }
                            button(type = ButtonType.submit, classes = "btn btn-primary mt-4") {
                                +"Create"
                                attrs.onClickFunction = { saveProject(it) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun RBuilder.inputTextFormRequired(
        form: InputTypes, classes: String,
        text: String,
        onChangeFun: (Event) -> Unit
    ) =
        div("$classes pl-2 pr-2") {
            label("form-label") {
                attrs.set("for", form.name)
                +text
            }

            if (form == InputTypes.GIT_TOKEN) {
                sup("tooltip-and-popover") {
                    fontAwesomeIcon(icon = faQuestionCircle)
                    attrs["tooltip-placement"] = "top"
                    attrs["tooltip-title"] = ""
                    attrs["popover-placement"] = "right"
                    attrs["popover-title"] = "Not working"
                    attrs["popover-content"] = "Not working"
                    attrs["data-trigger"] = "focus"
                    attrs["tabindex"] = "0"
                }
            }

            val validInput = when (form) {
                InputTypes.OWNER -> state.isValidOwner
                InputTypes.PROJECT_NAME -> state.isValidProjectName
                InputTypes.GIT_URL -> state.isValidGitUrl
                InputTypes.GIT_USER -> state.isValidGitUser
                InputTypes.GIT_TOKEN -> state.isValidGitToken
                else -> true
            }

            input(type = InputType.text) {
                attrs {
                    onChangeFunction = onChangeFun
                }
                attrs["id"] = form.name
                attrs["required"] = true
                if (validInput!!) {
                    attrs["class"] = "form-control"
                } else {
                    attrs["class"] = "form-control is-invalid"
                }
            }
            if (!validInput!!) {
                if (form == InputTypes.GIT_URL) {
                    div("invalid-feedback d-block") {
                        +"Input a valid URL. Note: spaces are not allowed and URL should start from http"
                    }
                } else {
                    div("invalid-feedback d-block") {
                        +"Please input a valid ${form.str}"
                    }
                }
            }
        }

    private fun RBuilder.inputTextFormOptional(
        form: InputTypes, classes: String,
        text: String,
        onChangeFun: (Event) -> Unit
    ) =
        div("$classes pl-2 pr-2") {
            label("form-label") {
                attrs.set("for", form.name)
                +text
            }
            div("input-group has-validation") {
                span("input-group-text") {
                    attrs["id"] = "${form.name}Span"
                    +"Optional"
                }
                input(type = InputType.text) {
                    attrs {
                        onChangeFunction = onChangeFun
                    }
                    attrs["aria-describedby"] = "${form.name}Span"
                    attrs["id"] = form.name
                    attrs["required"] = false
                    attrs["class"] = "form-control"
                }
            }
        }
}
