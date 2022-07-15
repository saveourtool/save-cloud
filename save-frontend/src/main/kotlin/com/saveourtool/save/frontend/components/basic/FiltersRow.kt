@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.domain.TestResultStatus
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

external interface FiltersRowProps : Props {
    /**
     * value status for filters table
     */
    var status: String

    /**
     * lambda for change status value
     */
    var onChangeStatus: (String) -> Unit

    /**
     * value file name
     */
    var fileName: String

    /**
     * lambda for change file name status
     */
    var onChangeTestName: (String) -> Unit

    /**
     * value test suite
     */
    var testSuite: String

    /**
     * lambda for change test suite value
     */
    var onChangeTestSuite: (String) -> Unit

    /**
     * value tag
     */
    var tag: String

    /**
     * lambda for change tag value
     */
    var onChangeTag: (String) -> Unit
}

/**
 * A row of filter selectors for table with `TestExecutionDto`s. Currently filters are "status" and "test suite".
 *
 * @return a function component
 */
@Suppress("TOO_LONG_FUNCTION", "TOO_MANY_PARAMETERS")
private fun testExecutionFiltersRow(
) = FC<FiltersRowProps> { props -> val (status, setStatus) = useState(props.status)
    val (fileName, setTestName) = useState(props.fileName)
    val (testSuite, setTestSuite) = useState(props.testSuite)
    val (tag, setTag) = useState(props.tag)
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
                                if (element == props.status) {
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
                        defaultValue = props.fileName
                        required = false
                        onChange = {
                            setTestName(it.target.value)
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
                        defaultValue = props.testSuite
                        required = false
                        onChange = {
                            setTestSuite(it.target.value)
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
                        defaultValue = props.tag
                        required = false
                        onChange = {
                            setTag(it.target.value)
                        }
                    }
                }
            }
            button {
                className = ClassName("btn btn-primary")
                fontAwesomeIcon(icon = faSearch, classes = "trash-alt")
                onClick = {
                    props.onChangeStatus(status)
                    props.onChangeTestName(fileName)
                    props.onChangeTestSuite(testSuite)
                    props.onChangeTag(tag)
                }
            }
            button {
                className = ClassName("btn btn-primary")
                fontAwesomeIcon(icon = faTrashAlt, classes = "trash-alt")
                onClick = {
                    props.onChangeStatus("ANY")
                    props.onChangeTestName("")
                    props.onChangeTestSuite("")
                    props.onChangeTag("")
                }
            }
        }
    }
}
