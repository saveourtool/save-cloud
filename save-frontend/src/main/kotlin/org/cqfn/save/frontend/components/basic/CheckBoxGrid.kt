/**
 * Grid with configurable number of checkboxes
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package org.cqfn.save.frontend.components.basic

import org.cqfn.save.frontend.externals.fontawesome.faQuestionCircle
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon
import org.cqfn.save.testsuite.TestSuiteDto

import react.PropsWithChildren
import react.dom.div
import react.dom.input
import react.dom.sup
import react.fc
import react.useEffect

import kotlinx.html.InputType
import kotlinx.html.classes
import kotlinx.html.js.onClickFunction
import react.dom.a
import react.dom.li
import react.dom.nav

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
    var selectedStandardSuites: MutableList<String>
}

fun suitesTable(suites: List<TestSuiteDto>) = fc<CheckBoxGridProps> { props ->
    nav("nav nav-tabs") {
        suites.map { it.language }
            .distinct()
            .forEachIndexed { index, s ->
                li("nav-item") {
                    a("nav-link") {
                        if (index == 0) {
                            // todo: store selected language in state?
                            attrs.classes += "active"
                        }
                        +(s ?: "N/A")
                    }
                }
            }
    }
}

/**
 * @param suites a list of [TestSuiteDto]s which should be displayed on the grid
 * @return an RComponent
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
fun checkBoxGrid(suites: List<TestSuiteDto>) = fc<CheckBoxGridProps> { props ->
    div {
        suites.chunked(props.rowSize)
            .forEach { row ->
                div("row") {
                    row.forEach { suite ->
                        div("col") {
                            +suite.name
                            sup("tooltip-and-popover") {
                                fontAwesomeIcon(icon = faQuestionCircle)
                                attrs["tooltip-placement"] = "top"
                                attrs["tooltip-title"] = suite.description?.take(100) ?: ""
                                attrs["popover-placement"] = "right"
                                attrs["popover-title"] = suite.name
                                attrs["popover-content"] = suiteDescription(suite)
                            }
                            input(type = InputType.checkBox, classes = "ml-3") {
                                attrs.defaultChecked = props.selectedStandardSuites.contains(suite.name)
                                attrs.onClickFunction = {
                                    if (props.selectedStandardSuites.contains(suite.name)) {
                                        props.selectedStandardSuites.remove(suite.name)
                                    } else {
                                        props.selectedStandardSuites.add(suite.name)
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
                content: jQuery(this).attr("popover-content"),
                html: true
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
