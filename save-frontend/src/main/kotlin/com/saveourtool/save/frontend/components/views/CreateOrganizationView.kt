/**
 * A view with organization creation details
 */

@file:Suppress("WildcardImport", "FILE_WILDCARD_IMPORTS")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.entities.*
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.inputform.inputTextFormRequired
import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.components.modal.mediumTransparentModalStyle
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.utils.*

import js.core.jso
import react.*
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.span
import web.cssom.ClassName
import web.cssom.rem
import web.html.ButtonType

import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * [State] of organization creation view component
 */
external interface OrganizationSaveViewState : State {
    /**
     * Flag to handle error
     */
    var isErrorWithOrganizationSave: Boolean

    /**
     * Error message
     */
    var errorMessage: String

    /**
     * Draft organization
     */
    var organizationDto: OrganizationDto

    /**
     * Conflict error message
     */
    var conflictErrorMessage: String?
}

/**
 * A functional Component for organization creation view
 *
 * @return a functional component
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class CreateOrganizationView : AbstractView<Props, OrganizationSaveViewState>(true) {
    init {
        state.isErrorWithOrganizationSave = false
        state.errorMessage = ""
        state.organizationDto = OrganizationDto.empty
        state.conflictErrorMessage = null
    }

    private fun saveOrganization() {
        scope.launch {
            val responseFromCreationOrganization = post(
                "$apiUrl/organizations/save",
                jsonHeaders,
                Json.encodeToString(state.organizationDto),
                loadingHandler = ::classLoadingHandler,
                responseHandler = ::classComponentResponseHandlerWithValidation,
            )
            if (responseFromCreationOrganization.ok) {
                window.location.href = "${window.location.origin}#/${state.organizationDto.name}/"
                window.location.reload()
            } else if (responseFromCreationOrganization.isConflict()) {
                val responseText = responseFromCreationOrganization.unpackMessage()
                setState {
                    conflictErrorMessage = responseText
                }
            } else if (!responseFromCreationOrganization.isUnauthorized()) {
                responseFromCreationOrganization.unpackMessage().let { message ->
                    setState {
                        isErrorWithOrganizationSave = true
                        errorMessage = message
                    }
                }
            }
        }
    }

    @Suppress(
        "TOO_LONG_FUNCTION",
        "EMPTY_BLOCK_STRUCTURE_ERROR",
        "LongMethod",
        "MAGIC_NUMBER"
    )
    override fun ChildrenBuilder.render() {
        displayModal(state.isErrorWithOrganizationSave, "Creation error", state.errorMessage, mediumTransparentModalStyle, { setState { isErrorWithOrganizationSave = false } }) {
            buttonBuilder("Close", "secondary") {
                setState { isErrorWithOrganizationSave = false }
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
                                    +"Create new organization"
                                }
                                img {
                                    className = ClassName("avatar avatar-user width-full border color-bg-default rounded-circle mb-4")
                                    src = "img/company.svg"
                                    style = jso {
                                        width = 8.rem
                                    }
                                }
                                form {
                                    className = ClassName("needs-validation")
                                    div {
                                        inputTextFormRequired {
                                            form = InputTypes.ORGANIZATION_NAME
                                            conflictMessage = state.conflictErrorMessage
                                            textValue = state.organizationDto.name
                                            validInput = (state.organizationDto.name.isEmpty() || state.organizationDto.validateName()) && state.conflictErrorMessage == null
                                            classes = ""
                                            name = "Organization name"
                                            onChangeFun = {
                                                setState {
                                                    organizationDto = organizationDto.copy(name = it.target.value)
                                                    conflictErrorMessage = null
                                                }
                                            }
                                        }
                                    }
                                    button {
                                        type = ButtonType.button
                                        className = ClassName("btn btn-info mt-4")
                                        +"Create organization"
                                        disabled = !state.organizationDto.validate() || state.conflictErrorMessage != null
                                        onClick = {
                                            saveOrganization()
                                        }
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
    companion object : RStatics<Props, OrganizationSaveViewState, CreateOrganizationView, Context<RequestStatusContext?>>(
        CreateOrganizationView::class
    ) {
        init {
            CreateOrganizationView.contextType = requestStatusContext
        }
    }
}
