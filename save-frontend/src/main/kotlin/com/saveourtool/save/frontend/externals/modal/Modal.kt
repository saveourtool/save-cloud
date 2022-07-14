/**
 * Kotlin/JS-React wrapper for react-modal: kotlin bindings
 */

package com.saveourtool.save.frontend.externals.modal

import react.ChildrenBuilder
import react.ReactDsl
import react.react
import kotlin.js.json

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

/**
 * @param block
 */
fun ChildrenBuilder.modal(
    block: @ReactDsl ChildrenBuilder.(ModalProps) -> Unit,
): Unit = ReactModal::class.react.invoke {
    style = defaultModalStyle
    shouldCloseOnOverlayClick = true
    block.invoke(this, this)
}
