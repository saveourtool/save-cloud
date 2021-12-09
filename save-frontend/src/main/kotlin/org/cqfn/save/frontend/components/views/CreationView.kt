
/**
 * A view with project creation details
 */

@file:Suppress("WildcardImport", "FILE_WILDCARD_IMPORTS")

package org.cqfn.save.frontend.components.views

import org.cqfn.save.entities.GitDto
import org.cqfn.save.entities.NewProjectDto
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.ProjectStatus
import org.cqfn.save.frontend.components.basic.InputTypes
import org.cqfn.save.frontend.components.basic.inputTextFormOptional
import org.cqfn.save.frontend.components.basic.inputTextFormRequired
import org.cqfn.save.frontend.utils.apiUrl
import org.cqfn.save.frontend.utils.get
import org.cqfn.save.frontend.utils.post
import org.cqfn.save.frontend.utils.runErrorModal

import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.Event
import org.w3c.fetch.Headers
import react.PropsWithChildren
import react.RBuilder
import react.State
import react.dom.*
import react.setState

import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.html.ButtonType
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

    /**
     * Validation of input fields
     */
    var gitConnectionCheckingStatus: GitConnectionStatusEnum?
}

/**
 * Special enum that stores the value with the result of testing git credentials
 */
enum class GitConnectionStatusEnum {
    CHECKED_NOT_OK,
    CHECKED_OK,
    INTERNAL_SERVER_ERROR,
    NOT_CHECKED,
    VALIDATING,
    ;
}

/**
 * A functional RComponent for project creation view
 *
 * @return a functional component
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class CreationView : AbstractView<PropsWithChildren, ProjectSaveViewState>(true) {
    private val fieldsMap: MutableMap<InputTypes, String> = mutableMapOf()

    init {
        state.isErrorWithProjectSave = false
        state.errorMessage = ""
        state.gitConnectionCheckingStatus = GitConnectionStatusEnum.NOT_CHECKED

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

    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
    private fun validateGitConnection() {
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        val urlArguments =
                "?user=${fieldsMap[InputTypes.GIT_USER]}&token=${fieldsMap[InputTypes.GIT_TOKEN]}&url=${fieldsMap[InputTypes.GIT_URL]}"

        GlobalScope.launch {
            setState {
                gitConnectionCheckingStatus = GitConnectionStatusEnum.VALIDATING
            }
            val responseFromCreationProject =
                    get("$apiUrl/check-git-connectivity-adaptor$urlArguments", headers)

            if (responseFromCreationProject.ok) {
                if (responseFromCreationProject.text().await().toBoolean()) {
                    setState {
                        gitConnectionCheckingStatus = GitConnectionStatusEnum.CHECKED_OK
                    }
                } else {
                    setState {
                        gitConnectionCheckingStatus = GitConnectionStatusEnum.CHECKED_NOT_OK
                    }
                }
            } else {
                setState {
                    gitConnectionCheckingStatus = GitConnectionStatusEnum.INTERNAL_SERVER_ERROR
                }
            }
        }
    }

    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
    private fun saveProject() {
        if (!isValidInput()) {
            return
        }
        val newProjectRequest = NewProjectDto(
            Project(
                fieldsMap[InputTypes.OWNER]!!.trim(),
                fieldsMap[InputTypes.PROJECT_NAME]!!.trim(),
                fieldsMap[InputTypes.PROJECT_URL]?.trim(),
                fieldsMap[InputTypes.DESCRIPTION]?.trim(),
                ProjectStatus.CREATED,
            ),
            GitDto(
                fieldsMap[InputTypes.GIT_URL]?.trim() ?: "",
                fieldsMap[InputTypes.GIT_USER]?.trim(),
                fieldsMap[InputTypes.GIT_TOKEN]?.trim(),
                fieldsMap[InputTypes.GIT_BRANCH]?.trim()
            )
        )
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        GlobalScope.launch {
            val responseFromCreationProject =
                    post("$apiUrl/saveProject", headers, Json.encodeToString(newProjectRequest))

            if (responseFromCreationProject.ok == true) {
                window.location.href =
                        "${window.location.origin}#/" +
                                "${newProjectRequest.project.owner.replace(" ", "%20")}/" +
                                newProjectRequest.project.name.replace(" ", "%20")
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
    @Suppress("TOO_LONG_FUNCTION", "SAY_NO_TO_VAR")
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
        } else {
            setState { isValidGitUser = true }
        }

        val gitToken = fieldsMap[InputTypes.GIT_TOKEN]
        if (gitToken.isNullOrBlank() || gitToken.trim().matches(".*\\s.*")) {
            setState { isValidGitToken = false }
        } else {
            setState { isValidGitToken = true }
        }

        val gitUrl = fieldsMap[InputTypes.GIT_URL]
        if (gitUrl.isNullOrBlank() || !gitUrl.trim().startsWith("http")) {
            setState { isValidGitUrl = false }
        } else {
            setState { isValidGitUrl = true }
        }
        return valid
    }

    @Suppress("TOO_LONG_FUNCTION", "EMPTY_BLOCK_STRUCTURE_ERROR", "LongMethod")
    override fun RBuilder.render() {
        runErrorModal(
            state.isErrorWithProjectSave,
            "Error appeared during project creation",
            state.errorMessage
        ) {
            setState { isErrorWithProjectSave = false }
        }

        main("main-content mt-0 ps") {
            div("page-header align-items-start min-vh-100") {
                span("mask bg-gradient-dark opacity-6") {}
                div("row justify-content-center") {
                    div("col-sm-5") {
                        div("container card o-hidden border-0 shadow-lg my-2 card-body p-0") {
                            div("p-5 text-center") {
                                h1("h4 text-gray-900 mb-4") {
                                    +"Create new test project"
                                }
                                form(classes = "needs-validation") {
                                    div("row g-3") {
                                        inputTextFormRequired(InputTypes.OWNER, state.isValidOwner!!, "col-md-6 pl-0 pl-2 pr-2", "Owner name") {
                                            changeFields(InputTypes.OWNER, it)
                                        }
                                        inputTextFormRequired(InputTypes.PROJECT_NAME, state.isValidProjectName!!, "col-md-6 pl-2 pr-2", "Tested tool name") {
                                            changeFields(InputTypes.PROJECT_NAME, it)
                                        }
                                        inputTextFormOptional(InputTypes.PROJECT_URL, "col-md-6 pr-0 mt-3", "Tested tool Url") {
                                            changeFields(InputTypes.PROJECT_URL, it)
                                        }
                                        inputTextFormOptional(
                                            InputTypes.GIT_URL,
                                            "col-md-6 mt-3 pl-0",
                                            "Test repository Git Url"
                                        ) {
                                            changeFields(InputTypes.GIT_URL, it, false)
                                        }
                                        inputTextFormOptional(InputTypes.GIT_USER, "col-md-6 mt-3", "Git Username") {
                                            changeFields(InputTypes.GIT_USER, it, false)
                                        }
                                        inputTextFormOptional(InputTypes.GIT_TOKEN, "col-md-6 mt-3 pr-0", "Git Token") {
                                            changeFields(InputTypes.GIT_TOKEN, it, false)
                                        }

                                        div("col-md-12 mt-3 mb-3 pl-0 pr-0") {
                                            label("form-label") {
                                                attrs.set("for", InputTypes.DESCRIPTION.name)
                                                +"Description"
                                            }
                                            div("input-group has-validation") {
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

                                        div("form-check form-switch") {
                                            input(classes = "form-check-input") {
                                                attrs["type"] = "checkbox"
                                                attrs["id"] = "isPublicSwitch"
                                                attrs["checked"] = "true"
                                            }
                                            label("form-check-label") {
                                                attrs["htmlFor"] = "isPublicSwitch"
                                                +"Public project"
                                            }
                                        }
                                    }

                                    button(type = ButtonType.submit, classes = "btn btn-info mt-4 mr-3") {
                                        +"Create test project"
                                        attrs.onClickFunction = { saveProject() }
                                    }
                                    button(type = ButtonType.button, classes = "btn btn-success mt-4 ml-3") {
                                        +"Validate connection"
                                        attrs.onClickFunction = { validateGitConnection() }
                                    }
                                    div("row justify-content-center") {
                                        when (state.gitConnectionCheckingStatus) {
                                            GitConnectionStatusEnum.CHECKED_NOT_OK ->
                                                createDiv(
                                                    "invalid-feedback d-block",
                                                    "Validation failed: please check your git URL and credentials"
                                                )
                                            GitConnectionStatusEnum.CHECKED_OK ->
                                                createDiv("valid-feedback d-block", "Successful validation of git configuration")
                                            GitConnectionStatusEnum.NOT_CHECKED ->
                                                createDiv("invalid-feedback d-block", "")
                                            GitConnectionStatusEnum.INTERNAL_SERVER_ERROR ->
                                                createDiv("invalid-feedback d-block", "Internal server error during git validation")
                                            GitConnectionStatusEnum.VALIDATING ->
                                                div("spinner-border spinner-border-sm mt-3") {
                                                    attrs["role"] = "status"
                                                }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun RBuilder.createDiv(blockName: String, text: String) =
            div("$blockName mt-2") {
                +text
            }
}
