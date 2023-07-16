package com.saveourtool.save.frontend.components.basic.table.filters

import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.buttonBuilder
import com.saveourtool.save.frontend.utils.withNavigate
import com.saveourtool.save.validation.FrontendRoutes
import react.FC
import react.Props
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.useEffect
import react.useState
import web.cssom.ClassName
import web.html.ButtonType
import web.html.InputType

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


val nameFiltersRow = FC<NameFilterRowProps> { props ->
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
                fontAwesomeIcon(icon = faSearch)
                onClick = {
                    props.onChangeFilters(filtersName)
                }
            }
            button {
                type = ButtonType.button
                className = ClassName("btn btn-secondary")
                fontAwesomeIcon(icon = faWindowClose)
                onClick = {
                    setFiltersName(null)
                    props.onChangeFilters(null)
                }
            }
        }
    }
}
