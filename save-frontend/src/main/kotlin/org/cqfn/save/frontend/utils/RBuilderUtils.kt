package org.cqfn.save.frontend.utils

import kotlinx.html.ButtonType
import kotlinx.html.js.onClickFunction
import org.cqfn.save.frontend.externals.modal.modal
import org.w3c.dom.events.Event
import react.RBuilder
import react.dom.button
import react.dom.div
import react.dom.h2

fun RBuilder.runErrorModal(
    isErrorOpen: Boolean,
    errorLabel: String,
    errorMessage: String,
    handler: (Event) -> Unit
) = modal {
    attrs {
        isOpen = isErrorOpen
        contentLabel = errorLabel
    }
    div {
        h2("h3 mb-0 text-gray-800") {
            +(errorMessage)
        }
    }
    button(type = ButtonType.button, classes = "btn btn-primary") {
        attrs.onClickFunction = handler
        +"Close"
    }
}
