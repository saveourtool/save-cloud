@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.frontend.components.basic.SelectOption.Companion.ANY
import com.saveourtool.save.frontend.externals.fontawesome.faFilter
import com.saveourtool.save.frontend.externals.fontawesome.faSearch
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

@Suppress("MISSING_KDOC_TOP_LEVEL", "UtilityClassWithPublicConstructor")
class SelectOption {
    companion object {
        const val ANY = "ANY"
    }
}

/**
 * A row of filter selectors for table with `TestExecutionDto`s. Currently filters are "status" and "test suite".
 *
 * @param initialValueStatus initial value of `TestResultStatus`
 * @param initialValueTestSuite initial value of `test suite`
 * @param onChangeStatus handler for selected value change
 * @param onChangeTestSuite handler for input value
 * @return a function component
 */
@Suppress("TOO_LONG_FUNCTION")
fun testExecutionFiltersRow(
    initialValueStatus: String,
    initialValueTestSuite: String,
    onChangeStatus: (String) -> Unit,
    onChangeTestSuite: (String) -> Unit,
) = FC<Props> {
    val (testSuite, setTestSuite) = useState(initialValueTestSuite)
    val (status, setStatus) = useState(initialValueStatus)
    div {
        className = ClassName("container-fluid")
        div {
            className = ClassName("row justify-content-start")
            div {
                className = ClassName("col-0 pr-1 align-self-center")
                fontAwesomeIcon(icon = faFilter)
            }
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
                            if (element == initialValueStatus) {
                                selected = true
                            }
                            +element
                        }
                    }

                    onChange = {
                        setStatus(it.target.value)
                    }
                }
            }
            div {
                className = ClassName("col-auto align-self-center")
                +"Test suite: "
            }
            div {
                className = ClassName("col-auto")
                input {
                    type = InputType.text
                    className = ClassName("form-control")
                    defaultValue = initialValueTestSuite
                    required = false
                    onChange = {
                        setTestSuite(it.target.value)
                    }
                }
            }
            button {
                className = ClassName("btn btn-primary")
                fontAwesomeIcon(icon = faSearch, classes = "trash-alt")
                // +"Find"
                onClick = {
                    onChangeStatus(status)
                    onChangeTestSuite(testSuite)
                }
            }
        }
    }
}
