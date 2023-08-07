@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.table.filters

import com.saveourtool.save.filters.VulnerabilityFilter
import com.saveourtool.save.frontend.components.inputform.inputWithDebounceForString
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.validation.FrontendRoutes
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.input
import web.cssom.ClassName
import web.html.InputType

val vulnerabilitiesFiltersRow: FC<VulnerabilitiesFiltersProps> = FC { props ->
    val (tagPrefix, setTagPrefix) = useState("")
    val (filter, setFilter) = useStateFromProps(props.filter)

    div {
        className = ClassName("px-0 container-fluid")
        div {
            className = ClassName("row d-flex")

            div {
                className = ClassName("col-0 mr-3 align-self-center")
                fontAwesomeIcon(icon = faFilter)
            }
            div {
                className = ClassName("row")
                div {
                    className = ClassName("col-auto mr-3")
                    input {
                        type = InputType.text
                        className = ClassName("form-control")
                        value = filter.prefixName
                        placeholder = "Name..."
                        required = false
                        onChange = { event ->
                            setFilter { oldFilter ->
                                oldFilter.copy(prefixName = event.target.value)
                            }
                        }
                    }
                }
                div {
                    className = ClassName("col-auto")
                    inputWithDebounceForString {
                        selectedOption = tagPrefix
                        setSelectedOption = { setTagPrefix(it) }
                        getUrlForOptionsFetch = { prefix -> "$apiUrl/tags/vulnerabilities?prefix=$prefix" }
                        placeholder = "Tag..."
                        renderOption = { childrenBuilder, tag ->
                            with(childrenBuilder) {
                                h6 {
                                    className = ClassName("text-sm")
                                    +tag
                                }
                            }
                        }
                        onOptionClick = { newTag ->
                            setFilter { oldFilter -> oldFilter.copy(tags = oldFilter.tags + newTag) }
                            setTagPrefix("")
                        }
                    }
                }
                div {
                    className = ClassName("col-auto align-middle row")
                    filter.tags.forEach { tag ->
                        buttonBuilder(tag, isOutline = tag !in props.filter.tags, classes = "rounded-pill text-sm btn-sm mx-1") {
                            setFilter { oldFilter -> oldFilter.copy(tags = filter.tags - tag) }
                        }
                    }
                }
            }

            div {
                className = ClassName("ml-auto")
                buttonBuilder(faSearch, classes = "btn mr-1", isOutline = props.filter == filter, style = "secondary") {
                    props.onChangeFilter(filter)
                }
                buttonBuilder(faWindowClose, classes = "btn mr-1", isOutline = true, style = "secondary") {
                    props.onChangeFilter(null)
                }

                withNavigate { navigateContext ->
                    buttonBuilder(faPlus, style = "primary", title = "Propose a new vulnerability", isOutline = true) {
                        navigateContext.navigate("/${FrontendRoutes.CREATE_VULNERABILITY}")
                    }
                }
            }
        }
    }
}

/**
 * [Props] for filters name
 */
external interface VulnerabilitiesFiltersProps : Props {
    /**
     * All [VulnerabilityFilter]
     */
    var filter: VulnerabilityFilter

    /**
     * [StateSetter] for [VulnerabilityFilter]
     */
    var onChangeFilter: (VulnerabilityFilter?) -> Unit
}
