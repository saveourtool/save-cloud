package org.cqfn.save.frontend.externals.switch

import react.RBuilder
import react.RHandler

fun RBuilder.rSwitch(
    handler: RHandler<ReactSwitchProps>
) = child(ReactSwitch::class) {
    attrs.checked = false
    attrs.onChange = { _, _, _ -> }
    handler.invoke(this)
}