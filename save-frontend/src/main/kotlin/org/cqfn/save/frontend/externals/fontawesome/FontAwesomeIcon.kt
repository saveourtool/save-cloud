@file:JsModule("@fortawesome/react-fontawesome")
@file:JsNonModule

package org.cqfn.save.frontend.externals.fontawesome

import react.Component
import react.ReactElement
import react.State

/**
 * External declaration of [FontAwesomeIcon] react component
 */
external class FontAwesomeIcon : Component<FontAwesomeIconProps, State> {
    override fun render(): ReactElement<FontAwesomeIconProps>?
}
