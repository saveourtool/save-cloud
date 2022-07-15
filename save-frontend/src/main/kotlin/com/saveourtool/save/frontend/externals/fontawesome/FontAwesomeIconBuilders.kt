/**
 * kotlin-react builders for FontAwesomeIcon components
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.externals.fontawesome

import react.ChildrenBuilder
import react.react

/**
 * A small wrapper for font awesome icons imported from individual modules.
 * See [faUser.d.ts](https://unpkg.com/browse/@fortawesome/free-solid-svg-icons@5.11.2/faUser.d.ts) as an example.
 * We only need [definition] field for using those icons, other fields could be added if needed.
 */
interface FontAwesomeIconModule {
    /**
     * Definition of FA icon ([IconDefinition] in terms of `@fortawesome/fontawesome-common-types`)
     */
    val definition: dynamic
}

/**
 * Builder function for new kotlin-react API
 *
 * @param icon
 * @param classes
 * @param handler
 */
fun ChildrenBuilder.fontAwesomeIcon(
    icon: FontAwesomeIconModule,
    classes: String = "",
    handler: ChildrenBuilder.(props: FontAwesomeIconProps) -> Unit = {},
): Unit = FontAwesomeIcon::class.react {
    this.icon = icon.definition
    this.className = classes
    this.handler(this)
}
