
/**
 * A view with project creation details
 */

@file:Suppress("WildcardImport", "FILE_WILDCARD_IMPORTS")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.entities.*
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.InputTypes
import com.saveourtool.save.frontend.components.basic.inputTextFormOptional
import com.saveourtool.save.frontend.components.basic.inputTextFormRequired
import com.saveourtool.save.frontend.components.basic.selectFormRequired
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.externals.fontawesome.faQuestionCircle
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.validation.FrontendRoutes
import com.saveourtool.save.validation.isValidName

import csstype.ClassName
import org.w3c.dom.*
import react.*
import react.dom.*
import react.dom.aria.ariaDescribedBy
import react.dom.html.ButtonType
import react.dom.html.InputType
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.textarea

import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * [State] of project creation view component
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
     * Draft [ProjectDto]
     */
    var projectCreationRequest: ProjectDto
}

/**
 * A functional Component for project creation view
 *
 * @return a functional component
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class CreationView : AbstractView<Props, ProjectSaveViewState>(true) {
    init {
        state.isErrorWithProjectSave = false
        state.errorMessage = ""
        state.projectCreationRequest = ProjectDto.empty
    }

    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION", "MAGIC_NUMBER")
    private fun saveProject() {
        scope.launch {
            val responseFromCreationProject =
                    post(
                        "$apiUrl/projects/save",
                        jsonHeaders,
                        Json.encodeToString(state.projectCreationRequest),
                        loadingHandler = ::classLoadingHandler,
                    )
            if (responseFromCreationProject.ok == true) {
                window.location.href = "${window.location.origin}#/${state.projectCreationRequest.organizationName}/${state.projectCreationRequest.name}"
                window.location.reload()
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

    @Suppress(
        "TOO_LONG_FUNCTION",
        "LongMethod",
    )
    override fun ChildrenBuilder.render() {
        runErrorModal(
            state.isErrorWithProjectSave,
            "Error appeared during project creation",
            state.errorMessage
        ) {
            setState { isErrorWithProjectSave = false }
        }

        main {
            className = ClassName("main-content mt-0 ps")
            div {
                className = ClassName("page-header align-items-start min-vh-100")
                span {
                    className = ClassName("mask bg-gradient-dark opacity-6")
                }
                div {
                    className = ClassName("row justify-content-center")
                    div {
                        className = ClassName("col-sm-4 mt-5")
                        div {
                            className = ClassName("container card o-hidden border-0 shadow-lg my-2 card-body p-0")
                            div {
                                className = ClassName("p-5 text-center")
                                h1 {
                                    className = ClassName("h4 text-gray-900 mb-4")
                                    +"Create new test project"
                                }
                                div {
                                    button {
                                        type = ButtonType.button
                                        className = ClassName("btn btn-primary mb-2")
                                        a {
                                            className = ClassName("text-light")
                                            href = "#/${FrontendRoutes.CREATE_ORGANIZATION.path}/"
                                            +"Add new organization"
                                        }
                                    }
                                }
                                form {
                                    className = ClassName("needs-validation")
                                    div {
                                        className = ClassName("row-3")
                                        selectFormRequired {
                                            form = InputTypes.ORGANIZATION_NAME
                                            validInput = state.projectCreationRequest.organizationName.isEmpty() || state.projectCreationRequest.organizationName.isValidName()
                                            classes = "col-md-12 pl-2 pr-2"
                                            text = "Organization"
                                            onChangeFun = {
                                                setState {
                                                    projectCreationRequest = projectCreationRequest.copy(organizationName = it.target.value)
                                                }
                                            }
                                        }
                                        inputTextFormRequired(
                                            InputTypes.PROJECT_NAME,
                                            state.projectCreationRequest.name,
                                            state.projectCreationRequest.name.isEmpty() || state.projectCreationRequest.validateProjectName(),
                                            "col-md-12 pl-2 pr-2",
                                            "Tested tool name",
                                        ) {
                                            setState {
                                                projectCreationRequest = projectCreationRequest.copy(name = it.target.value)
                                            }
                                        }
                                        inputTextFormOptional(
                                            InputTypes.PROJECT_URL,
                                            state.projectCreationRequest.url,
                                            "col-md-12 pl-2 pr-2 mt-3",
                                            "Tested Tool Website",
                                            validInput = state.projectCreationRequest.url.isEmpty() || state.projectCreationRequest.validateUrl(),
                                        ) {
                                            setState {
                                                projectCreationRequest = projectCreationRequest.copy(url = it.target.value)
                                            }
                                        }

                                        inputTextFormOptional(
                                            InputTypes.PROJECT_EMAIL,
                                            state.projectCreationRequest.email,
                                            "col-md-12 pl-2 pr-2 mt-3",
                                            "Tested Tool Email",
                                            validInput = state.projectCreationRequest.email.isEmpty() || state.projectCreationRequest.validateEmail(),
                                        ) {
                                            setState {
                                                projectCreationRequest = projectCreationRequest.copy(email = it.target.value)
                                            }
                                        }

                                        div {
                                            className = ClassName("col-md-12 mt-3 mb-3 pl-2 pr-2")
                                            label {
                                                className = ClassName("form-label")
                                                asDynamic()["for"] = InputTypes.DESCRIPTION.name
                                                +"Description"
                                            }
                                            div {
                                                className = ClassName("input-group has-validation")
                                                textarea {
                                                    className = ClassName("form-control")
                                                    onChange = {
                                                        setState {
                                                            projectCreationRequest = projectCreationRequest.copy(description = it.target.value)
                                                        }
                                                    }
                                                    ariaDescribedBy = "${InputTypes.DESCRIPTION.name}Span"
                                                    rows = 2
                                                    id = InputTypes.DESCRIPTION.name
                                                    required = false
                                                }
                                            }
                                        }

                                        div {
                                            className = ClassName("col-md-12 mt-3 mb-3 pl-2 pr-0 row")
                                            label {
                                                className = ClassName("text-xs")
                                                fontAwesomeIcon(icon = faQuestionCircle)
                                                asDynamic()["data-toggle"] = "tooltip"
                                                asDynamic()["data-placement"] = "top"
                                                title = "Private projects are visible for user, organization admins and selected users, " +
                                                        "while public ones are visible for everyone."
                                            }
                                            div {
                                                className = ClassName("col-5 text-left align-self-center")
                                                +"Project visibility:"
                                            }
                                            form {
                                                className = ClassName("col-7 form-group row d-flex justify-content-around")
                                                div {
                                                    className = ClassName("form-check-inline")
                                                    input {
                                                        className = ClassName("form-check-input")
                                                        defaultChecked = state.projectCreationRequest.isPublic
                                                        name = "projectVisibility"
                                                        type = InputType.radio
                                                        id = "isProjectPublicSwitch"
                                                        value = "true"
                                                    }
                                                    label {
                                                        className = ClassName("form-check-label")
                                                        htmlFor = "isProjectPublicSwitch"
                                                        +"Public"
                                                    }
                                                }
                                                div {
                                                    className = ClassName("form-check-inline")
                                                    input {
                                                        className = ClassName("form-check-input")
                                                        defaultChecked = !state.projectCreationRequest.isPublic
                                                        name = "projectVisibility"
                                                        type = InputType.radio
                                                        id = "isProjectPrivateSwitch"
                                                        value = "false"
                                                    }
                                                    label {
                                                        className = ClassName("form-check-label")
                                                        htmlFor = "isProjectPrivateSwitch"
                                                        +"Private"
                                                    }
                                                }
                                                onChange = {
                                                    setState {
                                                        projectCreationRequest = projectCreationRequest.copy(isPublic = (it.target as HTMLInputElement).value.toBoolean())
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    button {
                                        type = ButtonType.button
                                        className = ClassName("btn btn-info mt-4 mr-3")
                                        +"Create test project"
                                        disabled = !state.projectCreationRequest.validate()
                                        onClick = { saveProject() }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ChildrenBuilder.createDiv(blockName: String, text: String) =
            div {
                className = ClassName("$blockName mt-2")
                +text
            }

    companion object : RStatics<Props, ProjectSaveViewState, CreationView, Context<RequestStatusContext>>(CreationView::class) {
        init {
            contextType = requestStatusContext
        }
    }
}
