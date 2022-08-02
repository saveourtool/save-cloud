/**
 * A view with organization creation details
 */

@file:Suppress("WildcardImport", "FILE_WILDCARD_IMPORTS")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.entities.*
import com.saveourtool.save.frontend.components.basic.InputTypes
import com.saveourtool.save.frontend.components.basic.inputTextFormRequired
import com.saveourtool.save.frontend.utils.*

import csstype.ClassName
import react.*
import react.dom.*
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.span

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
    var isErrorWithOrganizationSave: Boolean?

    /**
     * Error message
     */
    var errorMessage: String

    /**
     * Draft organization
     */
    var organizationDto: OrganizationDto
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
    }

    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION", "MAGIC_NUMBER")
    private fun saveOrganization() {
        scope.launch {
            val responseFromCreationOrganization =
                    post(
                        "$apiUrl/organization/save",
                        jsonHeaders,
                        Json.encodeToString(state.organizationDto),
                        loadingHandler = ::classLoadingHandler,
                    )
            if (responseFromCreationOrganization.ok) {
                window.location.href = "${window.location.origin}#/${state.organizationDto.name.replace(" ", "%20")}/"
                window.location.reload()
            } else {
                responseFromCreationOrganization.text().then {
                    setState {
                        isErrorWithOrganizationSave = true
                        errorMessage = it
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
        runErrorModal(
            state.isErrorWithOrganizationSave,
            "Error appeared during organization creation",
            state.errorMessage
        ) {
            setState { isErrorWithOrganizationSave = false }
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
                                    className = ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
                                    src = "img/company.svg"
                                    height = 260.0
                                    width = 260.0
                                }
                                form {
                                    className = ClassName("needs-validation")
                                    div {
                                        inputTextFormRequired(
                                            InputTypes.ORGANIZATION_NAME,
                                            state.organizationDto.name,
                                            state.organizationDto.name.isEmpty() || state.organizationDto.validateName(),
                                            "",
                                            "Organization name",
                                        ) {
                                            setState {
                                                organizationDto = organizationDto.copy(name = it.target.value)
                                            }
                                        }
                                    }
                                    button {
                                        type = ButtonType.button
                                        className = ClassName("btn btn-info mt-4 mr-3")
                                        +"Create organization"
                                        disabled = !state.organizationDto.validate()
                                        onClick = {
                                            saveOrganization()
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
