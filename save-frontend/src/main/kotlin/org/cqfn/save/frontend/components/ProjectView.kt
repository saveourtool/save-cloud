package org.cqfn.save.frontend.components

import react.RBuilder
import react.RComponent
import react.RProps
import react.RState

class ProjectProps : RProps {
    lateinit var type: String  // todo type in common
    lateinit var name: String
    lateinit var owner: String
}

class ProjectView : RComponent<RProps, RState>() {
    override fun RBuilder.render() {

    }
}
