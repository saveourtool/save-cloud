/**
 * Utilities for kotlin-js RBuilder
 */

@file:Suppress("FILE_NAME_INCORRECT", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.frontend.externals.modal.modal

import org.w3c.dom.events.Event
import react.RBuilder
import react.dom.button
import react.dom.div
import react.dom.h2

import kotlinx.html.ButtonType
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
 * @param handler handler to event and close
 * @param handlerClose handler to close
 * @return modal
 */
fun RBuilder.runConfirmWindowModal(
    isConfirmWindowOpen: Boolean?,
    confirmLabel: String,
    confirmMessage: String,
    handlerClose: (Event) -> Unit,
    okButtonLabel: String = "Ok",
    closeButtonLabel: String = "Close",
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
