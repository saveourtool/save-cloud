@file:Suppress(
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.save.frontend.components.views.fossgraph

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.filters.VulnerabilityFilter
import com.saveourtool.save.frontend.TabMenuBar
import com.saveourtool.save.frontend.components.basic.fossGraphIntroductionComponent
import com.saveourtool.save.frontend.components.basic.nameFiltersRow
import com.saveourtool.save.frontend.components.tables.*
import com.saveourtool.save.frontend.components.views.contests.tab
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopResponseHandler
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes

import csstype.ClassName
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.tr
import react.router.dom.Link
import react.router.useNavigate

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * [FC] for fossGraphCollection view
 */
val fossGraphCollectionView: FC<FossGraphCollectionViewProps> = FC { props ->
    useBackground(Style.BLUE)
    val navigate = useNavigate()

    val (vulnerabilityFilters, setVulnerabilityFilters) = useState(VulnerabilityFilter.created)
    val (selectedMenu, setSelectedMenu) = useState(VulnerabilityListTab.PUBLIC)

    @Suppress(
        "TYPE_ALIAS",
        "MAGIC_NUMBER",
    )
    val fossGraphTable: FC<FiltersProps> = tableComponent(
        columns = {
            columns {
                column(id = "name", header = "Name", { name }) { cellContext ->
                    Fragment.create {
                        td {
                            Link {
                                to = "/${FrontendRoutes.FOSS_GRAPH}/${cellContext.row.original.name}"
                                +cellContext.value
                            }
                        }
                    }
                }
                column(id = "progress", header = "Critical", { progress }) { cellContext ->
                    Fragment.create {
                        td {
                            +"${ cellContext.row.original.progress }"
                        }
                    }
                }
            }
        },
        initialPageSize = 10,
        useServerPaging = false,
        usePageSelection = false,
        isTransparentGrid = true,
        getAdditionalDependencies = {
            arrayOf(it.filters)
        },
        commonHeader = { tableInstance, _ ->
            tr {
                th {
                    colSpan = tableInstance.visibleColumnsCount()
                    nameFiltersRow {
                        name = vulnerabilityFilters.prefixName
                        onChangeFilters = { filterValue ->
                            val filter = if (filterValue.isNullOrEmpty()) {
                                VulnerabilityFilter("", vulnerabilityFilters.active)
                            } else {
                                VulnerabilityFilter(filterValue, vulnerabilityFilters.active)
                            }
                            setVulnerabilityFilters { filter }

                            navigate(
                                to = buildString {
                                    "/${FrontendRoutes.FOSS_GRAPH}"
                                    filterValue?.let { append("?vulnerabilityName=$filterValue") }
                                }
                            )
                        }
                    }
                }
            }
        }
    )

    main {
        className = ClassName("main-content mt-0 ps")
        div {
            className = ClassName("page-header align-items-start min-vh-100")
            div {
                className = ClassName("d-flex justify-content-center")
                div {
                    className = ClassName("col-md-4 d-flex align-items-stretch")
                    div {
                        div {
                            className = ClassName("mb-2")
                            fossGraphIntroductionComponent()
                        }
                    }
                }
                div {
                    className = ClassName("card flex-md-row col-lg-7 d-flex align-items-stretch")
                    div {
                        className = ClassName("col")
                        div {
                            className = ClassName("d-flex justify-content-center mb-1 mt-2")
                            withNavigate { navigateContext ->
                                buttonBuilder(label = "Propose a new vulnerability", style = "info") {
                                    navigateContext.navigate("/${FrontendRoutes.CREATE_VULNERABILITY}")
                                }
                            }
                        }

                        div {
                            @Suppress("TOO_MANY_LINES_IN_LAMBDA")
                            props.currentUserInfo?.globalRole?.let { role ->
                                val tabList = if (role.isHigherOrEqualThan(Role.SUPER_ADMIN)) {
                                    VulnerabilityListTab.values().map { it.name }
                                } else {
                                    VulnerabilityListTab.values().filter { it != VulnerabilityListTab.ADMIN }
                                        .map { it.name }
                                }
                                tab(selectedMenu.name, tabList, "nav nav-tabs mt-3") { value ->
                                    setSelectedMenu { VulnerabilityListTab.valueOf(value) }
                                    setVulnerabilityFilters {
                                        when (VulnerabilityListTab.valueOf(value)) {
                                            VulnerabilityListTab.PUBLIC -> VulnerabilityFilter(
                                                prefixName = "",
                                                active = true
                                            )

                                            VulnerabilityListTab.ADMIN -> VulnerabilityFilter(
                                                prefixName = "",
                                                active = false
                                            )

                                            VulnerabilityListTab.OWNER -> VulnerabilityFilter(
                                                prefixName = "",
                                                active = false,
                                                isOwner = true
                                            )
                                        }
                                    }
                                }
                            }

                            fossGraphTable {
                                filters = vulnerabilityFilters
                                getData = { _, _ ->
                                    post(
                                        url = "$apiUrl/vulnerabilities/by-filters",
                                        headers = jsonHeaders,
                                        body = Json.encodeToString(vulnerabilityFilters),
                                        loadingHandler = ::noopLoadingHandler,
                                        responseHandler = ::noopResponseHandler,
                                    ).unsafeMap {
                                        it.decodeFromJsonString()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * `Props` for fossGraphCollectionView
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface FossGraphCollectionViewProps : Props {
    var currentUserInfo: UserInfo?
}

/**
 * Enum that contains values for vulnerability
 */
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class VulnerabilityListTab {
    PUBLIC,
    ADMIN,
    OWNER,
    ;

    companion object : TabMenuBar<VulnerabilityListTab> {
        override val nameOfTheHeadUrlSection = ""
        override val defaultTab: VulnerabilityListTab = PUBLIC
        override val regexForUrlClassification = "/${FrontendRoutes.FOSS_GRAPH.path}"
        override fun valueOf(elem: String): VulnerabilityListTab = VulnerabilityListTab.valueOf(elem)
        override fun values(): Array<VulnerabilityListTab> = VulnerabilityListTab.values()
    }
}

/**
 * `Props` for vulnerabilities table
 */
external interface FiltersProps : TableProps<VulnerabilityDto> {
    /**
     * All filters in one value [filters]
     */
    var filters: VulnerabilityFilter?
}
