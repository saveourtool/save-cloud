/**
 * kotlin-react builders for FontAwesomeIcon components
 */

package com.saveourtool.save.frontend.externals.fontawesome

import react.ChildrenBuilder
import react.RBuilder
import react.RHandler
import react.react

import kotlinx.js.jso

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

/**
 * Builder function for new kotlin-react API
 *
 * @param icon
 * @param classes
 * @param handler
 */
fun ChildrenBuilder.fontAwesomeIcon(
    icon: dynamic,
    classes: String = "",
    handler: ChildrenBuilder.(props: FontAwesomeIconProps) -> Unit = {},
): Unit = child(FontAwesomeIcon::class.react, props = jso {
    this.icon = icon
    this.className = classes
    handler(this)
})
