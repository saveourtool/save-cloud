@file:JsModule("@material-ui/icons")
@file:JsNonModule

package com.saveourtool.save.frontend.externals.material

import com.saveourtool.save.frontend.externals.fontawesome.FontAwesomeIconProps
import react.Component
import react.ReactElement
import react.State


external class MaterialUiIcon : Component<MaterialUiIconProps, State> {
    override fun render(): ReactElement<MaterialUiIconProps>?
}
