/**
 * Kotlin/JS-React wrapper for react-modal: kotlin bindings
 */

package org.cqfn.save.frontend.externals.modal

import react.RBuilder
import react.RHandler

/**
 * @param handler builder for modal component
 * @return a [ReactElement]
 */
fun RBuilder.modal(
    handler: RHandler<ModalProps>
) = child(ReactModal::class) {
    handler.invoke(this)
}
