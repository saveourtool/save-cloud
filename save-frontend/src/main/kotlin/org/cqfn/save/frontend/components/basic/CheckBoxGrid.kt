/**
 * Grid with configurable number of checkboxes
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package org.cqfn.save.frontend.components.basic

import react.PropsWithChildren
import react.dom.div
import react.dom.input
import react.fc

import kotlinx.html.InputType
import kotlinx.html.js.onClickFunction

/**
 * Props for ChecboxGrid component
 */
external interface CheckBoxGridProps : PropsWithChildren {
    /**
     * Length of row of checkboxes
     */
    var rowSize: Int

    /**
     * Currently selected elements
     */
    var selectedOptions: MutableList<String>
}

/**
 * @param options list of displayed selectable options
 * @return an RComponent
 */
fun checkBoxGrid(options: List<String>) = fc<CheckBoxGridProps> { props ->
    div {
        options.chunked(props.rowSize)
            .forEach { optionsRow ->
                div("row") {
                    optionsRow.forEach { option ->
                        div("col") {
                            +option
                            input(type = InputType.checkBox, classes = "ml-3") {
                                attrs.defaultChecked = props.selectedOptions.contains(option)
                                attrs.onClickFunction = {
                                    if (props.selectedOptions.contains(option)) {
                                        props.selectedOptions.remove(option)
                                    } else {
                                        props.selectedOptions.add(option)
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }
}
