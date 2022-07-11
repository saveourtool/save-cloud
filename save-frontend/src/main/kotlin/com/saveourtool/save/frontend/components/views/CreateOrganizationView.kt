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

import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.fetch.Headers
import react.dom.*

import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import react.dom.html.ButtonType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import react.*
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.span

/**
 * [RState] of organization creation view component
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
     * Validation of input fields
     */
    var isValidOrganizationName: Boolean?
}

/**
 * A functional RComponent for organization creation view
 *
 * @return a functional component
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class CreateOrganizationView : AbstractView<Props, OrganizationSaveViewState>(true) {
    private val fieldsMap: MutableMap<InputTypes, String> = mutableMapOf()

    init {
        state.isErrorWithOrganizationSave = false
        state.errorMessage = ""
        state.isValidOrganizationName = true
    }

    private fun changeFields(fieldName: InputTypes, target: Event, isOrganization: Boolean = true) {
        val tg = target.target as HTMLInputElement
        if (isOrganization) fieldsMap[fieldName] = tg.value else fieldsMap[fieldName] = tg.value
    }

    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION", "MAGIC_NUMBER")
    private fun saveOrganization() {
        if (!isValidInput()) {
            return
        }
        val organizationName = fieldsMap[InputTypes.ORGANIZATION_NAME]!!.trim()
        val dateCreated = LocalDateTime(1970, Month.JANUARY, 1, 0, 0, 1)

        val newOrganizationRequest = Organization(organizationName, OrganizationStatus.CREATED, null, dateCreated, null)
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        scope.launch {
            val responseFromCreationOrganization =
                    post(
                        "$apiUrl/organization/save",
                        headers,
                        Json.encodeToString(newOrganizationRequest),
                        loadingHandler = ::classLoadingHandler,
                    )
            if (responseFromCreationOrganization.ok) {
                window.location.href = "${window.location.origin}#/${organizationName.replace(" ", "%20")}/"
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

    @Suppress("SAY_NO_TO_VAR")
    private fun isValidInput(): Boolean {
        var valid = true
        val value = fieldsMap[InputTypes.ORGANIZATION_NAME]
        if (value.isInvalid(64)) {
            setState { isValidOrganizationName = false }
            valid = false
        } else {
            setState { isValidOrganizationName = true }
        }
        return valid
    }

    @Suppress("TOO_LONG_FUNCTION", "EMPTY_BLOCK_STRUCTURE_ERROR", "LongMethod")
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
                        className = ClassName("col-sm-4")
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
                                        inputTextFormRequired(InputTypes.ORGANIZATION_NAME, state.isValidOrganizationName!!, "", "Organization name", true) {
                                            changeFields(InputTypes.ORGANIZATION_NAME, it as Event)
                                        }
                                    }
                                    button {
                                        type = ButtonType.button
                                        className = ClassName("btn btn-info mt-4 mr-3")
                                        +"Create organization"
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
