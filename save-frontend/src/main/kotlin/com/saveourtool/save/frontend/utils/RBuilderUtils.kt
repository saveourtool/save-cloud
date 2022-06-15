/**
 * Utilities for kotlin-js RBuilder
 */

@file:Suppress("FILE_NAME_INCORRECT", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.frontend.components.basic.InputTypes
import com.saveourtool.save.frontend.externals.modal.modal

import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import react.RBuilder
import react.dom.*

import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.js.onClickFunction

/**
 * Enum that stores types of confirmation windows for different situations.
 */
enum class ConfirmationType {
    DELETE_CONFIRM,
    NO_BINARY_CONFIRM,
    NO_CONFIRM,
    ;
}

/**
 * @param isErrorOpen flag to handle error
 * @param errorLabel label of error
 * @param errorMessage message of error
 * @param closeButtonLabel label that will be shown on the only button
 * @param handler handler to close
 * @return modal
 */
fun RBuilder.runErrorModal(
    isErrorOpen: Boolean?,
    errorLabel: String,
    errorMessage: String,
    closeButtonLabel: String = "Close",
    handler: (Event) -> Unit
) = modal {
    attrs {
        isOpen = isErrorOpen
        contentLabel = errorLabel
    }
    div("row align-items-center justify-content-center") {
        h2("h6 text-gray-800") {
            +(errorMessage)
        }
    }
    div("d-sm-flex align-items-center justify-content-center mt-4") {
        button(type = ButtonType.button, classes = "btn btn-primary") {
            attrs.onClickFunction = handler
            +closeButtonLabel
        }
    }
}

/**
 * @param isConfirmWindowOpen flag to handle confirm Window
 * @param confirmLabel label of confirm Window
 * @param confirmMessage message
 * @param okButtonLabel label for ok button
 * @param closeButtonLabel label for close button
 * @param handlerClose handler to close
 * @param handler handler to event and close
 * @return modal
 */
@Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
fun RBuilder.runConfirmWindowModal(
    isConfirmWindowOpen: Boolean?,
    confirmLabel: String,
    confirmMessage: String,
    okButtonLabel: String = "Ok",
    closeButtonLabel: String = "Close",
    handlerClose: (Event) -> Unit,
    handler: (Event) -> Unit
) = modal {
    attrs {
        isOpen = isConfirmWindowOpen
        contentLabel = confirmLabel
    }
    div("row align-items-center justify-content-center") {
        h2("h6 text-gray-800 mb-2") {
            +(confirmMessage)
        }
    }
    div("d-sm-flex align-items-center justify-content-center mt-4") {
        button(type = ButtonType.button, classes = "btn btn-primary mr-3") {
            attrs.onClickFunction = handler
            +okButtonLabel
        }
        button(type = ButtonType.button, classes = "btn btn-outline-primary") {
            attrs.onClickFunction = handlerClose
            +closeButtonLabel
        }
    }
}

/**
 * @param isOpenGitWindow
 * @param gitDto
 * @param changeFields
 * @param handlerCancel
 * @param handler
 * @return modal
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "TYPE_ALIAS",
    "LongMethod",
)
fun RBuilder.runSettingGitWindow(
    isOpenGitWindow: Boolean?,
    gitDto: GitDto?,
    changeFields: (InputTypes, HTMLInputElement) -> Unit,
    handlerCancel: (Event) -> Unit,
    handler: (Event) -> Unit
) = modal {
    attrs {
        isOpen = isOpenGitWindow
    }

    div("row mt-2 ml-2 mr-2") {
        div("col-5 text-left align-self-center") {
            +"Git Username:"
        }
        div("col-7 input-group pl-0") {
            input(type = InputType.text) {
                attrs["class"] = "form-control"
                attrs {
                    defaultValue = gitDto?.username ?: ""
                    onChange = {
                        changeFields(
                            InputTypes.GIT_USER,
                            it.target as HTMLInputElement,
                        )
                    }
                }
            }
        }
    }
    div("row mt-2 ml-2 mr-2") {
        div("col-5 text-left align-self-center") {
            +"Git Url:"
        }
        div("col-7 input-group pl-0") {
            input(type = InputType.text) {
                attrs["class"] = "form-control"
                attrs {
                    defaultValue = gitDto?.url ?: ""
                    onChange = {
                        changeFields(
                            InputTypes.GIT_URL,
                            it.target as HTMLInputElement,
                        )
                    }
                }
            }
        }
    }
    div("row mt-2 ml-2 mr-2") {
        div("col-5 text-left align-self-center") {
            +"Git Token:"
        }
        div("col-7 input-group pl-0") {
            input(type = InputType.text) {
                attrs["class"] = "form-control"
                attrs {
                    onChange = {
                        changeFields(
                            InputTypes.GIT_TOKEN,
                            it.target as HTMLInputElement,
                        )
                    }
                }
            }
        }
    }
    div("d-sm-flex align-items-center justify-content-center mt-4") {
        button(type = ButtonType.button, classes = "btn btn-primary mr-3") {
            attrs.onClickFunction = handler
            +"Save"
        }
        button(type = ButtonType.button, classes = "btn btn-outline-primary") {
            attrs.onClickFunction = handlerCancel
            +"Cancel"
        }
    }
}
