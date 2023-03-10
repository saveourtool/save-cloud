/**
 * A view with global rating
 */

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.OrganizationWithRating
import com.saveourtool.save.entities.ProjectDto
import com.saveourtool.save.filters.OrganizationFilter
import com.saveourtool.save.filters.ProjectFilter
import com.saveourtool.save.frontend.components.basic.nameFiltersRow
import com.saveourtool.save.frontend.components.tables.TableProps
import com.saveourtool.save.frontend.components.tables.columns
import com.saveourtool.save.frontend.components.tables.pageIndex
import com.saveourtool.save.frontend.components.tables.pageSize
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.components.tables.value
import com.saveourtool.save.frontend.components.tables.visibleColumnsCount
import com.saveourtool.save.frontend.components.views.AbstractView
import com.saveourtool.save.frontend.externals.fontawesome.faTrophy
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.v1
import com.saveourtool.save.validation.FrontendRoutes

import csstype.ClassName
import csstype.rem
import history.Location
import js.core.jso
import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.tr

import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * `Props` retrieved from router
 */
external interface ContestGlobalRatingProps : Props {
    /**
     * Filters for project name
     */
    var projectName: String?

    /**
     * Filters for organization name
     */
    var organizationName: String?

    /**
     * Location for checking change url
     */
    var location: Location
}

/**
 * [State] of Contest Global Rating view component
 */
external interface ContestGlobalRatingViewState : State, HasSelectedMenu<UserRatingTab> {
    /**
     * All organizations
     */
    var organizationWithRatingList: Array<OrganizationWithRating>

    /**
     * All projects
     */
    var projects: Array<ProjectDto>

    /**
     * All filters for project
     */
    var projectFilter: ProjectFilter

    /**
     * All filters for organization
     */
    var organizationFilter: OrganizationFilter

    /**
     * Contains the paths of default and other tabs
     */
    var paths: PathsForTabs
}

/**
 * A Component for Contest Global Rating view
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class ContestGlobalRatingView : AbstractView<ContestGlobalRatingProps, ContestGlobalRatingViewState>(false) {
    @Suppress(
        "STRING_TEMPLATE_QUOTES",
        "TYPE_ALIAS",
    )
    private val tableWithOrganizationRating: FC<TableProps<OrganizationWithRating>> = tableComponent(
        columns = {
            columns {
                column(id = "index", header = "Position") {
                    Fragment.create {
                        td {
                            val index = it.row.index + 1 + it.pageIndex * it.pageSize
                            +"$index"
                        }
                    }
                }
                column(id = "name", header = "Name", { organization.name }) { cellContext ->
                    Fragment.create {
                        td {
                            a {
                                img {
                                    className =
                                            ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
                                    src = cellContext.row.original.organization.avatar?.let {
                                        "/api/$v1/avatar$it"
                                    } ?: "img/company.svg"
                                    style = jso {
                                        height = 2.rem
                                        width = 2.rem
                                    }
                                }
                                href = "#/${cellContext.value}"
                                +" ${cellContext.value}"
                            }
                        }
                    }
                }
                column(id = "rating", header = "Rating") { cellContext ->
                    Fragment.create {
                        td {
                            +cellContext.value.globalRating.toFixedStr(2)
                        }
                    }
                }
            }
        },
        isTransparentGrid = true,
        useServerPaging = false,
        usePageSelection = false,
        getAdditionalDependencies = {
            arrayOf(it)
        },
        commonHeader = { tableInstance, _ ->
            tr {
                th {
                    colSpan = tableInstance.visibleColumnsCount()
                    nameFiltersRow {
                        name = state.organizationFilter.prefix
                        onChangeFilters = { filterValue ->
                            val filter = if (filterValue.isNullOrEmpty()) {
                                OrganizationFilter.created
                            } else {
                                OrganizationFilter(filterValue)
                            }
                            setState {
                                organizationFilter = filter
                            }
                            getOrganization(filter)
                            window.location.href = buildString {
                                append(window.location.href.substringBefore("?"))
                                filterValue?.let { append("?organizationName=$filterValue") }
                            }
                        }
                    }
                }
            }
        }
    )

    @Suppress(
        "STRING_TEMPLATE_QUOTES",
        "TYPE_ALIAS",
    )
    private val renderingProjectChampionsTable: FC<TableProps<ProjectDto>> = tableComponent(
        columns = {
            columns<ProjectDto> {
                column(id = "index", header = "Position") {
                    Fragment.create {
                        td {
                            val index = it.row.index + 1 + it.pageIndex * it.pageSize
                            +"$index"
                        }
                    }
                }
                column(id = "name", header = "Name", { name }) { cellContext ->
                    Fragment.create {
                        td {
                            a {
                                href = "#/${cellContext.row.original.organizationName}/${cellContext.value}"
                                +" ${cellContext.value}"
                            }
                        }
                    }
                }
                column(id = "rating", header = "Rating") { cellContext ->
                    Fragment.create {
                        td {
                            +cellContext.value.contestRating.toFixedStr(2)
                        }
                    }
                }
            }
        },
        isTransparentGrid = true,
        useServerPaging = false,
        usePageSelection = false,
        getAdditionalDependencies = {
            arrayOf(it)
        },
        commonHeader = { tableInstance, _ ->
            tr {
                th {
                    colSpan = tableInstance.visibleColumnsCount()
                    nameFiltersRow {
                        name = state.projectFilter.name
                        onChangeFilters = { filterValue ->
                            val filter = if (filterValue.isNullOrEmpty()) {
                                ProjectFilter.created
                            } else {
                                ProjectFilter(filterValue)
                            }
                            setState {
                                projectFilter = filter
                            }
                            getProject(filter)
                            window.location.href = buildString {
                                append(window.location.href.substringBefore("?"))
                                filterValue?.let { append("?projectName=$filterValue") }
                            }
                        }
                    }
                }
            }
        }
    )

    init {
        state.organizationWithRatingList = emptyArray()
        state.projects = emptyArray()
        state.selectedMenu = UserRatingTab.defaultTab
        state.projectFilter = ProjectFilter.created
        state.organizationFilter = OrganizationFilter.created
    }

    private fun getOrganization(filterValue: OrganizationFilter) {
        scope.launch {
            val organizationsFromBackend: List<OrganizationWithRating> = post(
                url = "$apiUrl/organizations/by-filters-with-rating",
                headers = jsonHeaders,
                body = Json.encodeToString(filterValue),
                loadingHandler = ::classLoadingHandler,
            )
                .decodeFromJsonString()

            setState {
                organizationWithRatingList = organizationsFromBackend.toTypedArray()
            }
        }
    }

    private fun getProject(filterValue: ProjectFilter) {
        scope.launch {
            val projectsFromBackend: List<ProjectDto> = post(
                url = "$apiUrl/projects/by-filters",
                headers = jsonHeaders,
                body = Json.encodeToString(filterValue),
                loadingHandler = ::classLoadingHandler,
            )
                .decodeFromJsonString()

            setState {
                projects = projectsFromBackend.toTypedArray()
            }
        }
    }

    override fun componentDidUpdate(prevProps: ContestGlobalRatingProps, prevState: ContestGlobalRatingViewState, snapshot: Any) {
        if (state.selectedMenu != prevState.selectedMenu) {
            changeUrl(state.selectedMenu, UserRatingTab, state.paths)
            val href = window.location.href.substringBeforeLast("?")
            window.location.href = when (state.selectedMenu) {
                UserRatingTab.ORGS -> state.organizationFilter.prefix.let {
                    buildString {
                        append(href)
                        if (it.isNotBlank()) {
                            append("?organizationName=$it")
                        }
                    }
                }
                UserRatingTab.TOOLS -> state.projectFilter.name.let {
                    buildString {
                        append(href)
                        if (it.isNotBlank()) {
                            append("?projectName=$it")
                        }
                    }
                }
            }
        } else if (props.location != prevProps.location) {
            urlAnalysis(UserRatingTab, Role.NONE, false)
        }
    }

    override fun componentDidMount() {
        super.componentDidMount()
        val projectFilter = ProjectFilter(props.projectName ?: "")
        val organizationFilter = OrganizationFilter(props.organizationName.orEmpty())
        setState {
            paths = PathsForTabs("/${FrontendRoutes.CONTESTS_GLOBAL_RATING.path}", "#/${FrontendRoutes.CONTESTS_GLOBAL_RATING.path}")
            this.projectFilter = projectFilter
            this.organizationFilter = organizationFilter
        }
        urlAnalysis(UserRatingTab, Role.NONE, false)
        getOrganization(organizationFilter)
        getProject(projectFilter)
    }

    override fun ChildrenBuilder.render() {
        div {
            className = ClassName("d-sm-flex align-items-center justify-content-center mb-4")
            h1 {
                className = ClassName("h3 mb-0 text-gray-800")
                title(" Global Rating", faTrophy)
            }
        }

        tab(state.selectedMenu.name, UserRatingTab.values().map { it.name }) {
            setState {
                selectedMenu = UserRatingTab.valueOf(it)
            }
        }
        when (state.selectedMenu) {
            UserRatingTab.ORGS -> renderingOrganizationChampionsTable()
            UserRatingTab.TOOLS -> renderingProjectChampionsTable()
        }
    }

    private fun ChildrenBuilder.renderingOrganizationChampionsTable() {
        div {
            className = ClassName("row justify-content-center")
            div {
                className = ClassName("col-8")
                tableWithOrganizationRating {
                    getData = { _, _ ->
                        state.organizationWithRatingList
                    }
                    getPageCount = null
                }
            }
        }
    }

    private fun ChildrenBuilder.renderingProjectChampionsTable() {
        div {
            className = ClassName("row justify-content-center")
            div {
                className = ClassName("col-8")
                renderingProjectChampionsTable {
                    getData = { _, _ ->
                        state.projects
                    }
                    getPageCount = null
                }
            }
        }
    }
}
