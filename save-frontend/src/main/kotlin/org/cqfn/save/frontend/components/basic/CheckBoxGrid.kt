/**
 * Grid with configurable number of checkboxes
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package org.cqfn.save.frontend.components.basic

import kotlinx.browser.document
import react.PropsWithChildren
import react.dom.div
import react.dom.input
import react.fc

import kotlinx.html.InputType
import kotlinx.html.js.onClickFunction
import kotlinx.html.title
import org.w3c.dom.Node
import org.w3c.dom.asList
import react.useEffect
import react.useRef

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
fun checkBoxGrid(options: List<String>, tooltips: List<String?>) = fc<CheckBoxGridProps> { props ->
    div {
        options.zip(tooltips).chunked(props.rowSize)
            .forEach { row ->
                div("row") {
                    row.forEach { (option, tooltip) ->
                        div("col") {
                            +option
                            attrs["data-toggle"] = "tooltip"
                            attrs["data-placement"] = "top"
                            attrs.title = tooltip ?: ""
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
    useEffect(emptyList<dynamic>()) {
        js("var jQuery = require(\"jquery\")")
        js("jQuery('[data-toggle=\"tooltip\"]').tooltip()")
    }
}
