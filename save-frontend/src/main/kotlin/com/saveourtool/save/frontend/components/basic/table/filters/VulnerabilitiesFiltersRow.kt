@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.table.filters

import com.saveourtool.save.entities.OrganizationDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityLanguage
import com.saveourtool.save.filters.VulnerabilityFilter
import com.saveourtool.save.frontend.components.inputform.*
import com.saveourtool.save.frontend.components.inputform.renderUserWithAvatar
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import web.cssom.ClassName
import web.html.InputType

private const val LANGUAGE_PLACEHOLDER = "Language..."

private const val DROPDOWN_OPTIONS_AMOUNT = 3

val vulnerabilitiesFiltersRow: FC<VulnerabilitiesFiltersProps> = FC { props ->
    val (tagPrefix, setTagPrefix) = useState("")
    val (user, setUser) = useState(UserInfo(""))
    val (organization, setOrganization) = useState(OrganizationDto.empty)
    val (filter, setFilter) = useStateFromProps(props.filter)

    div {
        className = ClassName("px-0 container-fluid")
        div {
            className = ClassName("row d-flex justify-content-between")
            div {
                className = ClassName("col-0 mr-3 align-self-center")
                fontAwesomeIcon(icon = faFilter)
            }

            div {
                className = ClassName("row col-10 mb-1")
                div {
                    className = ClassName("col-3 px-1")
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
                    className = ClassName("col-1 px-1")
                    inputWithDebounceForString {
                        selectedOption = tagPrefix
                        setSelectedOption = { setTagPrefix(it) }
                        getUrlForOptionsFetch = { prefix ->
                            "$apiUrl/tags/vulnerabilities?prefix=$prefix&pageSize=$DROPDOWN_OPTIONS_AMOUNT"
                        }
                        placeholder = "Tag..."
                        renderOption = ::renderString
                        onOptionClick = { newTag ->
                            setFilter { oldFilter -> oldFilter.copy(tags = oldFilter.tags + newTag) }
                            setTagPrefix("")
                        }
                    }
                }

                div {
                    className = ClassName("col-3 px-1")
                    inputWithDebounceForUserInfo {
                        isDisabled = filter.authorName != null
                        selectedOption = user
                        setSelectedOption = { setUser(it) }
                        getUrlForOptionsFetch = { prefix ->
                            "$apiUrl/users/by-prefix?prefix=$prefix&pageSize=$DROPDOWN_OPTIONS_AMOUNT"
                        }
                        placeholder = "User..."
                        renderOption = ::renderUserWithAvatar
                        onOptionClick = { newUser ->
                            setUser(newUser)
                            setFilter { oldFilter -> oldFilter.copy(authorName = newUser.name) }
                        }
                    }
                }

                div {
                    className = ClassName("col-3 px-1")
                    inputWithDebounceForOrganizationDto {
                        isDisabled = filter.organizationName != null
                        selectedOption = organization
                        setSelectedOption = { setOrganization(it) }
                        getUrlForOptionsFetch = { prefix ->
                            "$apiUrl/organizations/get/by-prefix?prefix=$prefix&pageSize=$DROPDOWN_OPTIONS_AMOUNT"
                        }
                        placeholder = "Organization..."
                        renderOption = ::renderOrganizationWithAvatar
                        onOptionClick = { newOrganization ->
                            setOrganization(newOrganization)
                            setFilter { oldFilter -> oldFilter.copy(organizationName = newOrganization.name) }
                        }
                    }
                }

                div {
                    className = ClassName("col-auto px-1")
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
                className = ClassName("my-auto align-items-center")
                buttonBuilder(faSearch, classes = "btn mr-1", isOutline = props.filter == filter, style = "secondary") {
                    props.onChangeFilter(filter)
                }
                buttonBuilder(faWindowClose, classes = "btn mr-1", isOutline = true, style = "secondary") {
                    props.onChangeFilter(null)
                    setFilter { props.filter }
                    setTagPrefix("")
                    setOrganization(OrganizationDto.empty)
                    setUser(UserInfo(""))
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
