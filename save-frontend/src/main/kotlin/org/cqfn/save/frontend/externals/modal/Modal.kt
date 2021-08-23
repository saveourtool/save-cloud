/**
 * Kotlin/JS-React wrapper for react-modal: kotlin bindings
 */

package org.cqfn.save.frontend.externals.modal

import react.RBuilder
import react.RHandler
import kotlin.js.json

/**
 * @param handler builder for modal component
 * @return a `ReactElement`
 */
fun RBuilder.modal(
    handler: RHandler<ModalProps>
) = child(ReactModal::class) {
    attrs {
        style = Styles(
            // make modal window occupy center of the screen
            content = json(
                "top" to "25%",
                "left" to "25%",
                "right" to "auto",
                "bottom" to "auto"
            ).unsafeCast<CssProperties>()
        )
        shouldCloseOnOverlayClick = true
    }
    handler.invoke(this)
}
