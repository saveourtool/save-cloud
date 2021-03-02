package org.cqfn.save.frontend.components

import react.RBuilder
import react.RComponent
import react.RProps
import react.RState

external interface ProjectProps : RProps {
    var type: String  // todo type in common
    var name: String
    var owner: String
}

@OptIn(ExperimentalJsExport::class)
@JsExport
class ProjectView : RComponent<RProps, RState>() {
    override fun RBuilder.render() {

    }
}
