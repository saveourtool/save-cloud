@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.table.filters

import com.saveourtool.save.entities.vulnerability.VulnerabilityLanguage
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

private const val LANGUAGE_PLACEHOLDER = "Select a language..."

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
                    className = ClassName("col-auto")
                    selectorBuilder(
                        filter.language?.value ?: LANGUAGE_PLACEHOLDER,
                        VulnerabilityLanguage.values().map { it.value }.plus(LANGUAGE_PLACEHOLDER),
                        "form-control custom-select",
                    ) { event ->
                        val newLanguage = VulnerabilityLanguage.values().find { it.value == event.target.value }
                        setFilter { oldFilter -> oldFilter.copy(language = newLanguage) }
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
                    setFilter { props.filter }
                    setTagPrefix("")
                }

                withNavigate { navigateContext ->
                    buttonBuilder(faPlus, style = "primary", title = "Propose a new vulnerability", isOutline = true) {
                        navigateContext.navigate("/${FrontendRoutes.CREATE_VULNERABILITY}")
                    }
                }
            }
        }
        if (filter.tags.isNotEmpty()) {
            div {
                className = ClassName("row d-flex mt-2")
                filter.tags.forEach { tag ->
                    buttonBuilder(
                        tag,
                        if (tag !in props.filter.tags) "info" else "primary",
                        isOutline = true,
                        classes = "rounded-pill text-sm btn-sm mx-1 px-2"
                    ) {
                        setFilter { oldFilter -> oldFilter.copy(tags = filter.tags - tag) }
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
