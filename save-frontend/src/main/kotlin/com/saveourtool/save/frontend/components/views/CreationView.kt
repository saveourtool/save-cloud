
/**
 * A view with project creation details
 */

@file:Suppress("WildcardImport", "FILE_WILDCARD_IMPORTS")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.entities.*
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.selectFormRequired
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.inputform.inputTextFormOptional
import com.saveourtool.save.frontend.components.inputform.inputTextFormRequired
import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.components.modal.mediumTransparentModalStyle
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.externals.fontawesome.faQuestionCircle
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.validation.FrontendRoutes
import com.saveourtool.save.validation.isValidName

import react.*
import react.dom.*
import react.dom.aria.ariaDescribedBy
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
import web.cssom.ClassName
import web.html.*
import web.html.ButtonType
import web.html.InputType

import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Custom properties retrieved from the router when a project is being created.
 */
external interface ProjectSaveViewProps : Props {
    /**
     * The name of the parent organization, or `null` if the project is being
     * created from scratch.
     */
    var organizationName: String?
}

/**
 * [State] of project creation view component
 */
external interface ProjectSaveViewState : State {
    /**
     * Flag to handle error
     */
    var isErrorWithProjectSave: Boolean

    /**
     * Error message
     */
    var errorMessage: String

    /**
     * Draft [ProjectDto]
     */
    var projectCreationRequest: ProjectDto

    /**
     * Conflict error message
     */
    var conflictErrorMessage: String?
}

/**
 * A functional Component for project creation view
 *
 * @return a functional component
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class CreationView : AbstractView<ProjectSaveViewProps, ProjectSaveViewState>() {
    @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
    private val organizationSelectForm = selectFormRequired<String>()
    init {
        state.isErrorWithProjectSave = false
        state.errorMessage = ""
        state.projectCreationRequest = ProjectDto.empty
        state.conflictErrorMessage = null
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
                        responseHandler = ::classComponentResponseHandlerWithValidation,
                    )
            if (responseFromCreationProject.ok) {
                window.location.href = "${window.location.origin}#/${state.projectCreationRequest.organizationName}/${state.projectCreationRequest.name}"
                window.location.reload()
            } else if (responseFromCreationProject.isConflict()) {
                val responseText = responseFromCreationProject.unpackMessage()
                setState {
                    conflictErrorMessage = responseText
                }
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

    override fun componentDidMount() {
        super.componentDidMount()

        /*
         * Update the state if there's a parent organization available.
         */
        val organizationName = props.organizationName
        if (!organizationName.isNullOrEmpty()) {
            scope.launch {
                setState {
                    projectCreationRequest = projectCreationRequest.copy(organizationName = organizationName)
                }
            }
        }
    }

    @Suppress(
        "TOO_LONG_FUNCTION",
        "LongMethod",
    )
    override fun ChildrenBuilder.render() {
        displayModal(
            state.isErrorWithProjectSave,
            "Error appeared during project creation",
            state.errorMessage,
            mediumTransparentModalStyle,
            { setState { isErrorWithProjectSave = false } },
        ) {
            buttonBuilder("Close", "secondary") {
                setState { isErrorWithProjectSave = false }
            }
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
                                form {
                                    className = ClassName("needs-validation")
                                    div {
                                        className = ClassName("row-3")
                                        organizationSelectForm {
                                            selectClasses = "custom-select"
                                            formType = InputTypes.ORGANIZATION_NAME
                                            validInput = state.projectCreationRequest.organizationName.isEmpty() || state.projectCreationRequest.organizationName.isValidName()
                                            classes = "col-12 pl-2 pr-2"
                                            formName = "Organization"
                                            getData = { context ->
                                                context.get(
                                                    url = "$apiUrl/organizations/get/list",
                                                    headers = jsonHeaders,
                                                    loadingHandler = context::loadingHandler,
                                                )
                                                    .unsafeMap {
                                                        it.decodeFromJsonString<List<OrganizationDto>>()
                                                    }
                                                    .map {
                                                        it.name
                                                    }
                                            }
                                            dataToString = { it }
                                            selectedValue = state.projectCreationRequest.organizationName
                                            addNewItemChildrenBuilder = { childrenBuilder ->
                                                with(childrenBuilder) {
                                                    a {
                                                        href = "/#/${FrontendRoutes.CREATE_ORGANIZATION}"
                                                        +"Add new organization"
                                                    }
                                                }
                                            }
                                            disabled = false
                                            onChangeFun = { value ->
                                                setState {
                                                    projectCreationRequest = projectCreationRequest.copy(organizationName = value ?: "")
                                                }
                                            }
                                        }
                                        inputTextFormRequired {
                                            form = InputTypes.PROJECT_NAME
                                            textValue = state.projectCreationRequest.name
                                            validInput = (state.projectCreationRequest.name.isEmpty() || state.projectCreationRequest.validateProjectName()) &&
                                                    state.conflictErrorMessage == null
                                            classes = "col-12 pl-2 pr-2 mt-3 text-left"
                                            name = "Tested tool name"
                                            conflictMessage = state.conflictErrorMessage
                                            onChangeFun = {
                                                setState {
                                                    projectCreationRequest =
                                                            projectCreationRequest.copy(name = it.target.value)
                                                    conflictErrorMessage = null
                                                }
                                            }
                                        }

                                        inputTextFormOptional {
                                            form = InputTypes.PROJECT_EMAIL
                                            textValue = state.projectCreationRequest.email
                                            classes = "col-12 pl-2 pr-2 mt-3 text-left"
                                            name = "Contact e-mail"
                                            validInput =
                                                    state.projectCreationRequest.email.isEmpty() || state.projectCreationRequest.validateEmail()
                                            onChangeFun = {
                                                setState {
                                                    projectCreationRequest =
                                                            projectCreationRequest.copy(email = it.target.value)
                                                }
                                            }
                                        }

                                        div {
                                            className = ClassName("col-12 mt-3 mb-3 pl-2 pr-2 text-left")
                                            label {
                                                className = ClassName("form-label")
                                                asDynamic()["for"] = InputTypes.DESCRIPTION.name
                                                +"Description"
                                            }
                                            div {
                                                className = ClassName("input-group needs-validation")
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
                                            className = ClassName("col-12 mt-3 mb-3 pl-2 pr-0 row d-flex alighn-items-center")
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
                                                className = ClassName("col-7 form-group row d-flex justify-content-around align-items-center mb-0")
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
                                        className = ClassName("btn btn-info mt-4")
                                        +"Create test project"
                                        disabled = !state.projectCreationRequest.validate() || state.conflictErrorMessage != null
                                        onClick = { saveProject() }
                                    }

                                    state.conflictErrorMessage?.let {
                                        div {
                                            className = ClassName("invalid-feedback d-block")
                                            +it
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

    companion object : RStatics<ProjectSaveViewProps, ProjectSaveViewState, CreationView, Context<RequestStatusContext?>>(CreationView::class) {
        init {
            contextType = requestStatusContext
        }
    }
}
