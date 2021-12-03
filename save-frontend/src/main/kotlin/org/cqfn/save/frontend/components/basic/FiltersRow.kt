package org.cqfn.save.frontend.components.basic

import kotlinx.html.js.onChangeFunction
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.frontend.externals.fontawesome.faFilter
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon
import org.w3c.dom.HTMLSelectElement
import react.Props
import react.dom.div
import react.dom.option
import react.dom.select
import react.fc

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
