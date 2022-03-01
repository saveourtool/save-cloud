/**
 * kotlin-react builders for FontAwesomeIcon components
 */

package org.cqfn.save.frontend.externals.fontawesome

import kotlinx.js.jso
import react.ChildrenBuilder
import react.RBuilder
import react.RHandler
import react.react

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

fun ChildrenBuilder.fontAwesomeIcon(
    icon: dynamic,
    classes: String = "",
    handler: ChildrenBuilder.(props: FontAwesomeIconProps) -> Unit = {},
) = child(FontAwesomeIcon::class.react, props = jso {
    this.icon = icon
    this.className = classes
    handler(this)
})
