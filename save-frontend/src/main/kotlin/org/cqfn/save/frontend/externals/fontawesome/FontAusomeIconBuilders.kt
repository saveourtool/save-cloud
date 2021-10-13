/**
 * kotlin-react builders for FontAwesomeIcon components
 */

package org.cqfn.save.frontend.externals.fontawesome

import react.RBuilder
import react.RHandler

/**
 * @param icon icon. Can be an object, string or array.
 * @param classes element's classes
 * @param handler handler to set up a component
 * @return ReactElement
 */
fun RBuilder.fontAwesomeIcon(
    icon: dynamic,
    classes: String = "",
    handler: RHandler<FontAwesomeIconProps> = {},
) = child(FontAwesomeIcon::class) {
    attrs.icon = icon
    attrs.className = classes
    handler(this)
}

/**
 * @param handler handler to set up a component
 * @return ReactElement
 */
fun RBuilder.fontAwesomeIcon(
    handler: RHandler<FontAwesomeIconProps>
) = child(FontAwesomeIcon::class) {
    handler.invoke(this)
}
