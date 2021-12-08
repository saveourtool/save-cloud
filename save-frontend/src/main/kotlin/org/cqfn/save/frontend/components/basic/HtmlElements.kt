package org.cqfn.save.frontend.components.basic

import csstype.BorderRadius
import org.cqfn.save.entities.Project
import react.CSSProperties
import react.RBuilder
import react.dom.span

fun RBuilder.privacySpan(project: Project) {
    span("border ml-2 pr-1 pl-1 text-xs text-muted ") {
        attrs["style"] = kotlinext.js.jsObject<CSSProperties> {
            borderRadius = "2em".unsafeCast<BorderRadius>()
        }
        +if (project.public) "public" else "private"
    }
}
