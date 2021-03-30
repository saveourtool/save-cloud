/**
 * A component to display an error message in a modal view
 */

package org.cqfn.save.frontend.components.modal

import org.cqfn.save.frontend.externals.modal.ModalProps
import org.cqfn.save.frontend.externals.modal.modal

import react.RBuilder
import react.RHandler
import react.dom.button
import react.dom.div
import react.dom.h5
import react.dom.span

import kotlinx.html.ButtonType
import kotlinx.html.js.onClickFunction
import kotlinx.html.role

/**
 * @param errorHeader header of the modal window
 * @param errorText text of the error
 * @param handler a configuration for the component
 * @param closeCallback a callback to call when close button is pushed
 * @return a functional modal component
 */
fun RBuilder.errorModal(errorHeader: String,
                        errorText: String,
                        handler: RHandler<ModalProps>,
                        closeCallback: () -> Unit) = modal {
    handler(this)
    div("modal-dialog") {
        attrs.role = "document"
        div("modal-content") {
            div("modal-header") {
                h5("modal-title") {
                    +errorHeader
                }
                button(type = ButtonType.button, classes = "close") {
                    attrs {
                        set("data-dismiss", "modal")
                        set("aria-label", "Close")
                    }
                    span {
                        attrs["aria-hidden"] = "true"
                        attrs.onClickFunction = { closeCallback() }
                        +js("String.fromCharCode(215)").unsafeCast<String>()
                    }
                }
            }
        }

        div("modal-body") {
            +errorText
        }
    }
}
