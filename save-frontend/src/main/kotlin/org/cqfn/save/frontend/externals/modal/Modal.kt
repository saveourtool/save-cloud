/**
 * Kotlin/JS-React wrapper for react-modal: kotlin bindings
 */

package org.cqfn.save.frontend.externals.modal

import kotlinx.js.jso
import react.ChildrenBuilder
import react.IntrinsicType
import react.RBuilder
import react.RHandler
import react.ReactDsl
import react.react
import kotlin.js.json

/**
 * @param handler builder for modal component
 * @return a `ReactElement`
 */
fun RBuilder.modal(
    handler: RHandler<ModalProps>
) = child(ReactModal::class) {
    attrs {
        style = defaultModalStyle
        shouldCloseOnOverlayClick = true
    }
    handler.invoke(this)
}

fun ChildrenBuilder.modal(
    block: @ReactDsl ChildrenBuilder.(ModalProps) -> Unit,
) = ReactModal::class.react.unsafeCast<IntrinsicType<ModalProps>>().invoke {
    style = defaultModalStyle
    shouldCloseOnOverlayClick = true
    block.invoke(this, this)
}

private val defaultModalStyle = Styles(
    // make modal window occupy center of the screen
    content = json(
        "top" to "25%",
        "left" to "35%",
        "right" to "35%",
        "bottom" to "45%",
        "overflow" to "hide"
    ).unsafeCast<CssProperties>()
)