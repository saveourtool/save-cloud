package org.cqfn.save.frontend.externals.modal

import react.RBuilder
import react.RHandler

fun RBuilder.modal(
    handler: RHandler<ModalProps>
) = child(ReactModal::class) {
    handler.invoke(this)
}
