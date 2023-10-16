@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.table.filters

import com.saveourtool.save.entities.OrganizationDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityLanguage
import com.saveourtool.save.filters.VulnerabilityFilter
import com.saveourtool.save.frontend.components.inputform.*
import com.saveourtool.save.frontend.components.inputform.renderUserWithAvatar
import com.saveourtool.save.frontend.components.tables.TABLE_HEADERS_LOCALE_NAMESPACE
import com.saveourtool.save.frontend.components.views.vuln.component.uploadCosvButton
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.externals.i18next.useTranslation
import com.saveourtool.save.frontend.externals.slider.multiRangeSlider
import com.saveourtool.save.frontend.themes.Colors
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes
import js.core.jso
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import web.cssom.*
import web.html.InputType

private const val DROPDOWN_OPTIONS_AMOUNT = 3

@Suppress("IDENTIFIER_LENGTH")
val vulnerabilitiesFiltersRow: FC<VulnerabilitiesFiltersProps> = FC { props ->
    useTooltip()
    val (t) = useTranslation(TABLE_HEADERS_LOCALE_NAMESPACE)

    val (tagPrefix, setTagPrefix) = useState("")
    val (user, setUser) = useState(UserInfo(""))
    val (organization, setOrganization) = useState(OrganizationDto.empty)
    val (filter, setFilter) = useStateFromProps(props.filter)

    val languagePlaceholder = "Language".t()
    div {
        className = ClassName("px-0 container-fluid")
        div {
            className = ClassName("row d-flex justify-content-between")
            div {
                className = ClassName("col-0 mr-3 align-self-center")
                fontAwesomeIcon(icon = faFilter)
            }

            div {
                className = ClassName("col-11")

                div {
                    className = ClassName("row mt-3")
                    div {
                        className = ClassName("col-10")
                        div {
                            className = ClassName("row")
                            div {
                                className = ClassName("col-4 px-1")
                                input {
                                    type = InputType.text
                                    className = ClassName("form-control")
                                    value = filter.identifierPrefix
                                    placeholder = "${"Name".t()}..."
                                    required = false
                                    onChange = { event ->
                                        setFilter { oldFilter ->
                                            oldFilter.copy(identifierPrefix = event.target.value)
                                        }
                                    }
                                }
                            }

                            div {
                                className = ClassName("col-4 px-1")
                                multiRangeSlider {
                                    min = 0.0f
                                    max = 10.0f
                                    minValue = filter.minCriticality
                                    maxValue = filter.maxCriticality
                                    step = 0.1f
                                    stepOnly = true
                                    preventWheel = true
                                    ruler = false
                                    label = false
                                    style = jso {
                                        width = 16.4.rem
                                        height = 0.rem
                                        border = "none".unsafeCast<Border>()
                                        boxShadow = "none".unsafeCast<BoxShadow>()
                                    }
                                    barLeftColor = "red"
                                    barRightColor = "green"
                                    barInnerColor = Colors.WHITE.value
                                    thumbLeftColor = Colors.VULN_PRIMARY.value
                                    thumbRightColor = Colors.VULN_PRIMARY.value
                                    onChange = { changeResult ->
                                        setFilter { oldFilter -> oldFilter.copy(minCriticality = changeResult.minValue, maxCriticality = changeResult.maxValue) }
                                    }
                                }
                            }

                            div {
                                className = ClassName("col-4 px-1")
                                inputWithDebounceForUserInfo {
                                    isDisabled = filter.authorName != null
                                    selectedOption = user
                                    setSelectedOption = { setUser(it) }
                                    getUrlForOptionsFetch = { prefix ->
                                        "$apiUrl/users/by-prefix?prefix=$prefix&pageSize=$DROPDOWN_OPTIONS_AMOUNT"
                                    }
                                    placeholder = "${"COSV Submitter".t()}..."
                                    renderOption = ::renderUserWithAvatar
                                    onOptionClick = { newUser ->
                                        setUser(newUser)
                                        setFilter { oldFilter -> oldFilter.copy(authorName = newUser.name) }
                                    }
                                }
                            }
                        }
                    }

                    div {
                        className = ClassName("col-2")
                        buttonBuilder(faSearch, classes = "btn mr-1", isOutline = props.filter == filter, style = "secondary") {
                            props.onChangeFilter(filter)
                        }
                        buttonBuilder(faWindowClose, classes = "btn mr-1", title = "Drop filters", isOutline = true, style = "secondary") {
                            props.onChangeFilter(null)
                            // need to drop all tags
                            setFilter { props.filter.copy(tags = emptySet()) }
                            setTagPrefix("")
                            setOrganization(OrganizationDto.empty)
                            setUser(UserInfo(""))
                        }
                    }
                }

                div {
                    className = ClassName("row mt-1")

                    div {
                        className = ClassName("col-10")

                        div {
                            className = ClassName("row")
                            div {
                                className = ClassName("col-4 px-1")
                                inputWithDebounceForString {
                                    selectedOption = tagPrefix
                                    setSelectedOption = { setTagPrefix(it) }
                                    getUrlForOptionsFetch = { prefix ->
                                        "$apiUrl/tags/vulnerabilities?prefix=$prefix&pageSize=$DROPDOWN_OPTIONS_AMOUNT"
                                    }
                                    placeholder = "${"Tag".t()}..."
                                    renderOption = ::renderString
                                    onOptionClick = { newTag ->
                                        setFilter { oldFilter -> oldFilter.copy(tags = oldFilter.tags + newTag) }
                                        setTagPrefix("")
                                    }
                                }
                            }

                            div {
                                className = ClassName("col-4 px-1")
                                selectorBuilder(
                                    filter.language?.value ?: languagePlaceholder,
                                    VulnerabilityLanguage.values().map { it.value }.plus(languagePlaceholder),
                                    "form-control custom-select",
                                ) { event ->
                                    val newLanguage = VulnerabilityLanguage.values().find { it.value == event.target.value }
                                    setFilter { oldFilter -> oldFilter.copy(language = newLanguage) }
                                }
                            }

                            div {
                                className = ClassName("col-4 px-1")
                                inputWithDebounceForOrganizationDto {
                                    isDisabled = filter.organizationName != null
                                    selectedOption = organization
                                    setSelectedOption = { setOrganization(it) }
                                    getUrlForOptionsFetch = { prefix ->
                                        "$apiUrl/organizations/get/by-prefix?prefix=$prefix&pageSize=$DROPDOWN_OPTIONS_AMOUNT"
                                    }
                                    placeholder = "${"Organization".t()}..."
                                    renderOption = ::renderOrganizationWithAvatar
                                    onOptionClick = { newOrganization ->
                                        setOrganization(newOrganization)
                                        setFilter { oldFilter -> oldFilter.copy(organizationName = newOrganization.name) }
                                    }
                                }
                            }
                        }
                    }

                    div {
                        className = ClassName("col-2")
                        withNavigate { navigateContext ->
                            buttonBuilder(faPlus, style = "primary mr-1", isOutline = true) {
                                navigateContext.navigate("/${FrontendRoutes.CREATE_VULNERABILITY}")
                            }
                        }
                        uploadCosvButton {
                            isImage = true
                        }
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
                        "info",
                        isOutline = true,
                        classes = "rounded-pill text-sm btn-sm mx-1 px-2"
                    ) {
                        val newFilter = filter.copy(tags = filter.tags - tag)
                        setFilter { newFilter }
                        props.onChangeFilter(newFilter)
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
