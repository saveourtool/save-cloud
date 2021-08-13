package org.cqfn.save.frontend.components.basic

import kotlinx.html.InputType
import kotlinx.html.js.onClickFunction
import org.cqfn.save.frontend.components.views.ProjectView
import react.RProps
import react.dom.div
import react.dom.input
import react.fc

external interface CheckBoxGridProps : RProps {
    var rowSize: Int
    var selectedTypes: MutableList<String>
}

fun checkBoxGrid(options: List<String>) = fc<CheckBoxGridProps> { props ->
    div {
        options
            .chunked(props.rowSize)
            .forEach { rowTypes ->
                div("row") {
                    rowTypes.forEach { typeName ->
                        div("col") {
                            +typeName
                            input(type = InputType.checkBox, classes = "ml-3") {
                                attrs.defaultChecked = props.selectedTypes.contains(typeName)
                                attrs.onClickFunction = {
                                    if (props.selectedTypes.contains(typeName)) {
                                        props.selectedTypes.remove(typeName)
                                    } else {
                                        props.selectedTypes.add(typeName)
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }
}
