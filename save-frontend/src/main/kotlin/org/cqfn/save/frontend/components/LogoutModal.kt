/**
 * A component for logout modal window
 */

package org.cqfn.save.frontend.components

import org.cqfn.save.frontend.externals.modal.ModalProps
import org.cqfn.save.frontend.externals.modal.modal

import react.RBuilder
import react.RHandler
import react.dom.a
import react.dom.button
import react.dom.div
import react.dom.h5
import react.dom.span

import kotlinx.html.ButtonType
import kotlinx.html.js.onClickFunction
import kotlinx.html.role

/**
 * @param handler a [RHandler] for [ModalProps]
 * @param closeCallback a callback to call to close the modal
 * @return a Component
 */
@Suppress("TOO_LONG_FUNCTION")
fun RBuilder.logoutModal(handler: RHandler<ModalProps>, closeCallback: () -> Unit) = modal {
    handler(this)
    div("modal-dialog") {
        attrs.role = "document"
        div("modal-content") {
            div("modal-header") {
                h5("modal-title") {
                    +"Ready to Leave?"
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
            +"Select \"Logout\" below if you are ready to end your current session."
        }
        div("modal-footer") {
            button(type = ButtonType.button, classes = "btn btn-secondary") {
                attrs["data-dismiss"] = "modal"
                // FixMe: doesn't close now
                attrs.onClickFunction = { closeCallback() }
                +"Cancel"
            }
            a(classes = "btn btn-primary") {
                +"Logout"
            }
        }
    }
}
