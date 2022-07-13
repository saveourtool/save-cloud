/**
 * A component to display an error message in a modal view
 */

package com.saveourtool.save.frontend.components.modal

import com.saveourtool.save.frontend.externals.modal.ModalProps
import com.saveourtool.save.frontend.externals.modal.modal

import csstype.ClassName
import react.ChildrenBuilder
import react.RBuilder
import react.RHandler
import react.dom.aria.AriaRole
import react.dom.aria.ariaHidden
import react.dom.aria.ariaLabel
import react.dom.attrs
import react.dom.button
import react.dom.div
import react.dom.h5
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.span
import react.dom.span

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
                        closeCallback: () -> Unit,
) = modal {
    handler(this)
    div("modal-dialog") {
        attrs.role = "document"
        div("modal-content") {
            div("modal-header") {
                h5("modal-title") {
                    +errorHeader
                }
                button(type = kotlinx.html.ButtonType.button, classes = "close") {
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

/**
 * @param errorHeader header of the modal window
 * @param errorText text of the error
 * @param handler a configuration for the component
 * @param closeCallback a callback to call when close button is pushed
 * @return a functional modal component
 */
@Suppress("TOO_MANY_LINES_IN_LAMBDA")
fun ChildrenBuilder.errorModal(
    errorHeader: String,
    errorText: String,
    handler: ChildrenBuilder.(ModalProps) -> Unit,
    closeCallback: () -> Unit,
) = modal { props ->
    handler(props)
    div {
        className = ClassName("modal-dialog")
        role = "document".unsafeCast<AriaRole>()
        div {
            className = ClassName("modal-content")
            div {
                className = ClassName("modal-header")
                h5 {
                    className = ClassName("modal-title")
                    +errorHeader
                }
                button {
                    type = ButtonType.button
                    className = ClassName("close")
                    asDynamic()["data-dismiss"] = "modal"
                    ariaLabel = "Close"
                    span {
                        ariaHidden = true
                        onClick = { closeCallback() }
                        +js("String.fromCharCode(215)").unsafeCast<String>()
                    }
                }
            }
        }

        div {
            className = ClassName("modal-body")
            +errorText
        }
    }
}
