/**
 * File that contains html elements that are used multiple times in the project
 */

package org.cqfn.save.frontend.components.basic

import org.cqfn.save.entities.Project

import csstype.BorderRadius
import react.CSSProperties
import react.RBuilder
import react.dom.span

/**
 * @param project
 */
fun RBuilder.privacySpan(project: Project) {
    span("border ml-2 pr-1 pl-1 text-xs text-muted ") {
        attrs["style"] = kotlinext.js.jso<CSSProperties> {
            borderRadius = "2em".unsafeCast<BorderRadius>()
        }
        +if (project.public) "public" else "private"
    }
}
