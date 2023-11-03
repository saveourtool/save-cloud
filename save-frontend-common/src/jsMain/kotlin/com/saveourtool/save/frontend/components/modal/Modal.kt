/**
 * Kotlin/JS-React wrapper for react-modal: kotlin bindings
 */

package com.saveourtool.save.frontend.components.modal

import com.saveourtool.save.frontend.externals.modal.ModalProps
import com.saveourtool.save.frontend.externals.modal.ReactModal
import com.saveourtool.save.frontend.externals.modal.Styles
import react.ChildrenBuilder
import react.ReactDsl
import react.react

/**
 * @param injectedStyle css style that you can add to the modal window
 * @param block invoked properties
 */
fun ChildrenBuilder.modal(
    injectedStyle: Styles = defaultModalStyle,
    block: @ReactDsl ChildrenBuilder.(ModalProps) -> Unit,
): Unit = ReactModal::class.react.invoke {
    style = injectedStyle
    shouldCloseOnOverlayClick = true
    block.invoke(this, this)
}
