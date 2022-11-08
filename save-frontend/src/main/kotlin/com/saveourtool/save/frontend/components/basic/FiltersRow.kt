@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.entities.OrganizationStatus
import com.saveourtool.save.filters.OrganizationAdminFilters
import com.saveourtool.save.filters.OrganizationFilters
import com.saveourtool.save.filters.TestExecutionFilters
import com.saveourtool.save.frontend.components.basic.SelectOption.Companion.ANY
import com.saveourtool.save.frontend.externals.fontawesome.faFilter
import com.saveourtool.save.frontend.externals.fontawesome.faSearch
import com.saveourtool.save.frontend.externals.fontawesome.faTrashAlt
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon

import csstype.ClassName
import react.FC
import react.Props
import react.dom.html.ButtonType
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.useEffect

import react.useState

val testExecutionFiltersRow = testExecutionFiltersRow()

val nameFiltersRow = nameFiltersRow()

val organizationAdminRow = organizationAdminFiltersRow()

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
 * [Props] for filters name
 */
external interface NameFilterRowProps : Props {
    /**
     * All filters in one class property [name]
     */
    var name: String?

    /**
     * lambda to change [name]
     */
    var onChangeFilters: (String?) -> Unit
}


/**
 * [Props] for filters name
 */
external interface OrganizationAdminFilterRowProps : Props {
    /**
     * All filters in one class property [name]
     */
    var filters: OrganizationAdminFilters

    /**
     * lambda to change [name]
     */
    var onChangeFilters: (OrganizationAdminFilters) -> Unit
}

/**
 * A row of filter selectors for table with `TestExecutionDto`s. Currently filters are "status" and "test suite".
 *
 * @return a function component
 */
@Suppress("LongMethod", "TOO_LONG_FUNCTION")
private fun testExecutionFiltersRow(
) = FC<FiltersRowProps> { props ->
    // Store local copy of filters in order to perform searching only by the search button, and not by any change in the filter fields
    val (filters, setFilters) = useState(props.filters)
    useEffect(props.filters) {
        if (filters !== props.filters) {
            setFilters(props.filters)
        }
    }
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
                        value = filters.status?.name ?: ANY
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
                        value = filters.fileName ?: ""
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
                        value = filters.testSuite ?: ""
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
                        value = filters.tag ?: ""
                        required = false
                        onChange = {
                            setFilters(filters.copy(tag = it.target.value))
                        }
                    }
                }
            }
            button {
                type = ButtonType.button
                className = ClassName("btn btn-primary")
                fontAwesomeIcon(icon = faSearch, classes = "trash-alt")
                onClick = {
                    props.onChangeFilters(filters)
                }
            }
            button {
                type = ButtonType.button
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
private fun nameFiltersRow(
) = FC<NameFilterRowProps> { props ->

    val (filtersName, setFiltersName) = useState(props.name)
    useEffect(props.name) {
        if (filtersName != props.name) {
            setFiltersName(props.name)
        }
    }

    div {
        className = ClassName("container-fluid")
        div {
            className = ClassName("row d-flex")
            div {
                className = ClassName("col-0 mr-3 align-self-center")
                fontAwesomeIcon(icon = faFilter)
            }
            div {
                className = ClassName("row")
                div {
                    className = ClassName("col-auto align-self-center")
                    +"Name: "
                }
                div {
                    className = ClassName("col-auto mr-3")
                    input {
                        type = InputType.text
                        className = ClassName("form-control")
                        value = filtersName ?: ""
                        required = false
                        onChange = {
                            setFiltersName(it.target.value)
                        }
                    }
                }
            }
            button {
                type = ButtonType.button
                className = ClassName("btn btn-secondary mr-3")
                fontAwesomeIcon(icon = faSearch, classes = "trash-alt")
                onClick = {
                    props.onChangeFilters(filtersName)
                }
            }
            button {
                type = ButtonType.button
                className = ClassName("btn btn-secondary")
                fontAwesomeIcon(icon = faTrashAlt, classes = "trash-alt")
                onClick = {
                    setFiltersName(null)
                    props.onChangeFilters(null)
                }
            }
        }
    }
}


private fun organizationAdminFiltersRow() = FC<OrganizationAdminFilterRowProps> { props ->
    // Store local copy of filters in order to perform searching only by the search button, and not by any change in the filter fields
    val (filters, setFilters) = useState(props.filters)
    useEffect(props.filters) {
        if (filters !== props.filters) {
            setFilters(props.filters)
        }
    }
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
                        option {
                            disabled = true
                            selected = false
                            value = "ANY"
                        }
                        val elements = OrganizationStatus.values().map { it.name }.toMutableList()
                        elements.forEach { element ->
                            option {
                                +element
                                if (props.filters.contains(OrganizationStatus.valueOf(element))) {
                                    selected = true
                                }
                            }
                        }
                        onChange = {
                            setFilters(filters.copy(status = props.filters.printChangeStatus(it.target.value)))
                        }
                    }
                }
            }
            div {
                className = ClassName("row")
                div {
                    className = ClassName("col-auto align-self-center")
                    +"Organization name: "
                }
                div {
                    className = ClassName("col-auto")
                    input {
                        type = InputType.text
                        className = ClassName("form-control")
                        value = filters.organizationName
                        required = false
                        onChange = {
                            setFilters(filters.copy(organizationName = it.target.value))
                        }
                    }
                }
            }
            button {
                type = ButtonType.button
                className = ClassName("btn btn-primary")
                fontAwesomeIcon(icon = faSearch, classes = "trash-alt")
                onClick = {
                    props.onChangeFilters(filters)
                }
            }
            button {
                type = ButtonType.button
                className = ClassName("btn btn-primary")
                fontAwesomeIcon(icon = faTrashAlt, classes = "trash-alt")
                onClick = {
                    setFilters(OrganizationAdminFilters.empty)
                    props.onChangeFilters(OrganizationAdminFilters.empty)
                }
            }
        }
    }
}