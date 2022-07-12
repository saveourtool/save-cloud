/**
 * File that contains html elements that are used multiple times in the project
 */

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.entities.Project

import csstype.BorderRadius
import react.CSSProperties
import react.ChildrenBuilder
import react.RBuilder
import react.dom.html.ReactHTML.span
import react.dom.span

import kotlinx.js.jso

/**
 * @param project
 */
fun ChildrenBuilder.privacySpan(project: Project) {
    span {
        className = ClassName("border ml-2 pr-1 pl-1 text-xs text-muted ")
        style = jso {
            borderRadius = "2em".unsafeCast<BorderRadius>()
        }
        +if (project.public) "public" else "private"
    }
}

/**
 * @param project
 */
fun RBuilder.privacySpan(project: Project) {
    span("border ml-2 pr-1 pl-1 text-xs text-muted ") {
        attrs["style"] = jso<CSSProperties> {
            borderRadius = "2em".unsafeCast<BorderRadius>()
        }
        +if (project.public) "public" else "private"
    }
}
