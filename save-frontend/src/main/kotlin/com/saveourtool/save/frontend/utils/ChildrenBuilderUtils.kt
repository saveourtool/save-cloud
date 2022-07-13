/**
 * Utilities for kotlin-js ChildrenBuilder
 */

@file:Suppress("FILE_NAME_INCORRECT", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.frontend.externals.modal.modal

import csstype.ClassName
import org.w3c.dom.HTMLButtonElement
import react.ChildrenBuilder
import react.dom.events.MouseEventHandler
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2

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
@Suppress("TOO_MANY_LINES_IN_LAMBDA")
fun ChildrenBuilder.runErrorModal(
    isErrorOpen: Boolean?,
    errorLabel: String,
    errorMessage: String,
    closeButtonLabel: String = "Close",
    handler: MouseEventHandler<HTMLButtonElement>
) = modal { props ->
    props.isOpen = isErrorOpen
    props.contentLabel = errorLabel
    div {
        className = ClassName("row align-items-center justify-content-center")
        h2 {
            className = ClassName("h6 text-gray-800")
            +(errorMessage)
        }
    }
    div {
        className = ClassName("d-sm-flex align-items-center justify-content-center mt-4")
        button {
            type = ButtonType.button
            className = ClassName("btn btn-primary")
            onClick = handler
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
fun ChildrenBuilder.runConfirmWindowModal(
    isConfirmWindowOpen: Boolean?,
    confirmLabel: String,
    confirmMessage: String,
    okButtonLabel: String = "Ok",
    closeButtonLabel: String = "Close",
    handlerClose: MouseEventHandler<HTMLButtonElement>,
    handler: MouseEventHandler<HTMLButtonElement>,
) = modal { props ->
    props.isOpen = isConfirmWindowOpen
    props.contentLabel = confirmLabel
    div {
        className = ClassName("row align-items-center justify-content-center")
        h2 {
            className = ClassName("h6 text-gray-800 mb-2")
            +(confirmMessage)
        }
    }
    div {
        className = ClassName("d-sm-flex align-items-center justify-content-center mt-4")
        button {
            type = ButtonType.button
            className = ClassName("btn btn-primary mr-3")
            onClick = handler
            +okButtonLabel
        }
        button {
            type = ButtonType.button
            className = ClassName("btn btn-outline-primary")
            onClick = handlerClose
            +closeButtonLabel
        }
    }
}
