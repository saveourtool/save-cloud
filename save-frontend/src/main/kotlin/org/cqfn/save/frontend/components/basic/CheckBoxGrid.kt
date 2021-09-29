/**
 * Grid with configurable number of checkboxes
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package org.cqfn.save.frontend.components.basic

import org.cqfn.save.frontend.externals.fontawesome.faQuestionCircle
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon

import react.PropsWithChildren
import react.dom.div
import react.dom.input
import react.dom.sup
import react.fc
import react.useEffect

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
 * @param tooltips
 * @return an RComponent
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
fun checkBoxGrid(options: List<String>, tooltips: List<String?>) = fc<CheckBoxGridProps> { props ->
    div {
        options.zip(tooltips).chunked(props.rowSize)
            .forEach { row ->
                div("row") {
                    row.forEach { (option, tooltip) ->
                        div("col") {
                            +option
                            sup("tooltip-and-popover") {
                                fontAwesomeIcon(icon = faQuestionCircle)
                                attrs["tooltip-placement"] = "top"
                                attrs["tooltip-title"] = tooltip?.take(100) ?: ""
                                attrs["popover-placement"] = "right"
                                attrs["popover-title"] = option
                                attrs["popover-content"] = tooltip ?: ""
                            }
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
        js("""jQuery('.tooltip-and-popover').each(function() {
            jQuery(this).popover({
                placement: jQuery(this).attr("popover-placement"),
                title: jQuery(this).attr("popover-title"),
                content: jQuery(this).attr("popover-content")
            }).tooltip({
                placement: jQuery(this).attr("tooltip-placement"), 
                title: jQuery(this).attr("tooltip-title")
            }).on('show.bs.popover', function() {
                jQuery(this).tooltip('hide')
            }).on('hide.bs.popover', function() {
                jQuery(this).tooltip('show')
            })
        })""")
    }
}
