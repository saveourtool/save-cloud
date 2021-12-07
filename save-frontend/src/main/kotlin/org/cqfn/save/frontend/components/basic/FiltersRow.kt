@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package org.cqfn.save.frontend.components.basic

import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.frontend.externals.fontawesome.faFilter
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon

import org.w3c.dom.HTMLSelectElement
import react.Props
import react.dom.div
import react.dom.option
import react.dom.select
import react.fc

import kotlinx.html.js.onChangeFunction

/**
 * A row of filter selectors for table with `TestExecutionDto`s. Currently the only filter is "status".
 *
 * @param initialValue initial value of `TestResultStatus`
 * @param onChange handler for selected value change
 * @return a function component
 */
fun testExecutionFiltersRow(
    initialValue: String,
    onChange: (String) -> Unit,
) = fc<Props> {
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
                    option(content = "ANY")
                    TestResultStatus.values().forEach {
                        option(content = it.name)
                    }
                    attrs.value = initialValue
                    attrs.onChangeFunction = {
                        val target = it.target as HTMLSelectElement
                        onChange(target.value)
                    }
                }
            }
        }
    }
}
