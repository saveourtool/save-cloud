@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.execution.TestExecutionFilters
import com.saveourtool.save.frontend.components.basic.SelectOption.Companion.ANY
import com.saveourtool.save.frontend.externals.fontawesome.faFilter
import com.saveourtool.save.frontend.externals.fontawesome.faSearch
import com.saveourtool.save.frontend.externals.fontawesome.faTrashAlt
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon

import csstype.ClassName
import react.FC
import react.Props
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.useState

val testExecutionFiltersRow = testExecutionFiltersRow()

@Suppress("MISSING_KDOC_TOP_LEVEL", "UtilityClassWithPublicConstructor")
class SelectOption {
    companion object {
        const val ANY = "ANY"
    }
}

/**
 * [Props] for filters value
 */
external interface FiltersRowProps : Props {
    /**
     * All filters in one class property [filters]
     */
    var filters: TestExecutionFilters

    /**
     * lambda to change [filters]
     */
    var onChangeFilters: (TestExecutionFilters) -> Unit
}

/**
 * A row of filter selectors for table with `TestExecutionDto`s. Currently filters are "status" and "test suite".
 *
 * @return a function component
 */
@Suppress("LongMethod", "TOO_LONG_FUNCTION")
private fun testExecutionFiltersRow(
) = FC<FiltersRowProps> { props ->
    val(filters, setFilters) = useState(props.filters)
    div {
        className = ClassName("container-fluid")
        div {
            className = ClassName("row d-flex justify-content-between")
            div {
                className = ClassName("col-0 pr-1 align-self-center")
                fontAwesomeIcon(icon = faFilter)
            }
            div {
                className = ClassName("row")
                div {
                    className = ClassName("col-auto align-self-center")
                    +"Status: "
                }
                div {
                    className = ClassName("col-auto")
                    select {
                        className = ClassName("form-control")
                        val elements = TestResultStatus.values().map { it.name }.toMutableList()
                        elements.add(0, ANY)
                        elements.forEach { element ->
                            option {
                                if (element == props.filters.status?.name) {
                                    selected = true
                                }
                                +element
                            }
                        }
                        onChange = {
                            if (it.target.value == "ANY") {
                                setFilters(filters.copy(status = null))
                            } else {
                                setFilters(filters.copy(status = TestResultStatus.valueOf(it.target.value)))
                            }
                        }
                    }
                }
            }
            div {
                className = ClassName("row")
                div {
                    className = ClassName("col-auto align-self-center")
                    +"File name: "
                }
                div {
                    className = ClassName("col-auto")
                    input {
                        type = InputType.text
                        className = ClassName("form-control")
                        value = filters.fileName
                        required = false
                        onChange = {
                            setFilters(filters.copy(fileName = it.target.value))
                        }
                    }
                }
            }
            div {
                className = ClassName("row")
                div {
                    className = ClassName("col-auto align-self-center")
                    +"Test suite: "
                }
                div {
                    className = ClassName("col-auto")
                    input {
                        type = InputType.text
                        className = ClassName("form-control")
                        value = filters.testSuite
                        required = false
                        onChange = {
                            setFilters(filters.copy(testSuite = it.target.value))
                        }
                    }
                }
            }
            div {
                className = ClassName("row")
                div {
                    className = ClassName("col-auto align-self-center")
                    +"Tags: "
                }
                div {
                    className = ClassName("col-auto")
                    input {
                        type = InputType.text
                        className = ClassName("form-control")
                        value = filters.tag
                        required = false
                        onChange = {
                            setFilters(filters.copy(tag = it.target.value))
                        }
                    }
                }
            }
            button {
                className = ClassName("btn btn-primary")
                fontAwesomeIcon(icon = faSearch, classes = "trash-alt")
                onClick = {
                    props.onChangeFilters(filters)
                }
            }
            button {
                className = ClassName("btn btn-primary")
                fontAwesomeIcon(icon = faTrashAlt, classes = "trash-alt")
                onClick = {
                    setFilters(TestExecutionFilters.empty)
                    props.onChangeFilters(TestExecutionFilters.empty)
                }
            }
        }
    }
}
