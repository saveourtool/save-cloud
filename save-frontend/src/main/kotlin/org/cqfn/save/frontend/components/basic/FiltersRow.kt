@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package org.cqfn.save.frontend.components.basic

import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.frontend.externals.fontawesome.faFilter
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon

import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import react.Props
import react.dom.button
import react.dom.defaultValue
import react.dom.div
import react.dom.input
import react.dom.option
import react.dom.select
import react.fc

import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction

/**
 * A row of filter selectors for table with `TestExecutionDto`s. Currently the only filter is "status".
 *
 * @param initialValue initial value of `TestResultStatus`
 * @param onChange handler for selected value change
 * @param initialValueTestSuite
 * @param onChangeTestSuite
 * @return a function component
 */
@Suppress("TOO_LONG_FUNCTION")
fun testExecutionFiltersRow(
    initialValue: String,
    initialValueTestSuite: String,
    onChange: (String) -> Unit,
    onChangeTestSuite: (String) -> Unit,
) = fc<Props> {
    var status: String = initialValue
    var testSuite: String = initialValueTestSuite
    div("container-fluid") {
        div("row justify-content-start") {
            div("col-0 pr-1 align-self-center") {
                fontAwesomeIcon(icon = faFilter)
            }
            div("col-auto align-self-center") {
                +"Status: "
            }
            div("col-auto") {
                select("form-control") {
                    val elements = TestResultStatus.values().map { it.name }.toMutableList()
                    elements.add(0, "ANY")
                    elements.forEach { element ->
                        option {
                            if (element == initialValue) {
                                attrs.selected = true
                            }
                            +element
                        }
                    }

                    attrs.onChangeFunction = {
                        status = (it.target as HTMLSelectElement).value
                    }
                }
            }
            div("col-auto align-self-center") {
                +"Test suite: "
            }
            div("col-auto") {
                input(type = InputType.text) {
                    attrs.defaultValue = initialValueTestSuite
                    attrs.required = false
                    attrs.onChangeFunction = {
                        testSuite = (it.target as HTMLInputElement).value
                    }
                }
            }
            button(classes = "btn btn-primary") {
                +"Find"
                attrs.onClickFunction = {
                    onChange(status)
                    onChangeTestSuite(testSuite)
                }
            }
        }
    }
}
