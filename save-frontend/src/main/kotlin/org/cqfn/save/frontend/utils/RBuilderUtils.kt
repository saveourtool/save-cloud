/**
 * Utilities for kotlin-js RBuilder
 */

@file:Suppress("FILE_NAME_INCORRECT")

package org.cqfn.save.frontend.utils

import org.cqfn.save.frontend.externals.modal.modal

import org.w3c.dom.events.Event
import react.RBuilder
import react.dom.button
import react.dom.div
import react.dom.h2

import kotlinx.html.ButtonType
import kotlinx.html.js.onClickFunction

/**
 * @param isErrorOpen flag to handle error
 * @param errorLabel label of error
 * @param errorMessage message of error
 * @param handler handler to close
 * @return modal
 */
fun RBuilder.runErrorModal(
    isErrorOpen: Boolean?,
    errorLabel: String,
    errorMessage: String,
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
            +"Close"
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
            +"Ok"
        }
        button(type = ButtonType.button, classes = "btn btn-outline-primary") {
            attrs.onClickFunction = handlerClose
            +"Close"
        }
    }
}
