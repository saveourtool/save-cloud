@file:JsModule("react-switch")
@file:JsNonModule

package org.cqfn.save.frontend.externals.switch

import react.Component
import react.Props
import react.PropsWithChildren
import react.ReactElement
import react.State

external interface ReactSwitchProps : Props {
    var checked: Boolean?

    var onChange: (
        checked: Boolean,
        event: dynamic,
        id: String,
    ) -> Unit

    var uncheckedIcon: ReactElement?

    var checkedIcon: ReactElement?

}

@JsName("default")
external class ReactSwitch : Component<ReactSwitchProps, State> {
    override fun render(): ReactElement?
}