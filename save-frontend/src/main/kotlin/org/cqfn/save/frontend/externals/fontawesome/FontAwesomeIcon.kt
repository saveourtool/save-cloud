@file:JsModule("@fortawesome/react-fontawesome")
@file:JsNonModule

package org.cqfn.save.frontend.externals.fontawesome

import react.Component
import react.RState
import react.ReactElement

/**
 * External declaration of [FontAwesomeIcon] react component
 */
external class FontAwesomeIcon : Component<FontAwesomeIconProps, RState> {
    override fun render(): ReactElement?
}
