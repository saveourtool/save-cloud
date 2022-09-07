/**
 * A view with global rating
 */

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.Project
import com.saveourtool.save.filters.OrganizationFilters
import com.saveourtool.save.filters.ProjectFilters
import com.saveourtool.save.frontend.components.basic.nameFiltersRow
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.components.views.AbstractView
import com.saveourtool.save.frontend.externals.fontawesome.faTrophy
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.v1
import com.saveourtool.save.validation.FrontendRoutes

import csstype.ClassName
import csstype.rem
import history.Location
import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.tr
import react.table.columns

import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.js.jso
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
    var organizations: Array<Organization>

    /**
     * All projects
     */
    var projects: Array<Project>

    /**
     * All filters for project
     */
    var projectFilters: ProjectFilters

    /**
     * All filters for organization
     */
    var organizationFilters: OrganizationFilters
}

/**
 * A Component for Contest Global Rating view
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class ContestGlobalRatingView : AbstractView<ContestGlobalRatingProps, ContestGlobalRatingViewState>(false) {
    @Suppress(
        "STRING_TEMPLATE_QUOTES",
    )
    private val tableWithOrganizationRating = tableComponent(
        columns = columns<Organization> {
            column(id = "index", header = "Position") {
                Fragment.create {
                    td {
                        val index = it.row.index + 1 + it.state.pageIndex * it.state.pageSize
                        +"$index"
                    }
                }
            }
            column(id = "name", header = "Name", { name }) { cellProps ->
                Fragment.create {
                    td {
                        a {
                            img {
                                className =
                                        ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
                                src = cellProps.row.original.avatar?.let {
                                    "/api/$v1/avatar$it"
                                } ?: "img/company.svg"
                                style = jso {
                                    height = 2.rem
                                    width = 2.rem
                                }
                            }
                            href = "#/${cellProps.value}"
                            +" ${cellProps.value}"
                        }
                    }
                }
            }
            column(id = "rating", header = "Rating") {
                Fragment.create {
                    td {
                        +"4560"
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
        commonHeader = { tableInstance ->
            tr {
                th {
                    colSpan = tableInstance.columns.size
                    nameFiltersRow {
                        name = state.organizationFilters.name
                        onChangeFilters = { filterValue ->
                            val filter = if (filterValue.isNullOrEmpty()) {
                                OrganizationFilters(null)
                            } else {
                                OrganizationFilters(filterValue)
                            }
                            setState {
                                organizationFilters = filter
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
    )
    private val renderingProjectChampionsTable = tableComponent(
        columns = columns<Project> {
            column(id = "index", header = "Position") {
                Fragment.create {
                    td {
                        val index = it.row.index + 1 + it.state.pageIndex * it.state.pageSize
                        +"$index"
                    }
                }
            }
            column(id = "name", header = "Name", { name }) { cellProps ->
                Fragment.create {
                    td {
                        a {
                            href = "#/${cellProps.row.original.organization.name}/${cellProps.value}"
                            +" ${cellProps.value}"
                        }
                    }
                }
            }
            column(id = "rating", header = "Rating") {
                Fragment.create {
                    td {
                        +"1370"
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
        commonHeader = { tableInstance ->
            tr {
                th {
                    colSpan = tableInstance.columns.size
                    nameFiltersRow {
                        name = state.projectFilters.name
                        onChangeFilters = { filterValue ->
                            val filter = if (filterValue.isNullOrEmpty()) {
                                ProjectFilters(null)
                            } else {
                                ProjectFilters(filterValue)
                            }
                            setState {
                                projectFilters = filter
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
        state.organizations = emptyArray()
        state.projects = emptyArray()
        state.selectedMenu = UserRatingTab.defaultTab
        state.projectFilters = ProjectFilters(null)
        state.organizationFilters = OrganizationFilters(null)
    }

    private fun getOrganization(filterValue: OrganizationFilters) {
        scope.launch {
            val organizationsFromBackend: List<Organization> = post(
                url = "$apiUrl/organizations/not-deleted",
                headers = jsonHeaders,
                body = Json.encodeToString(filterValue),
                loadingHandler = ::classLoadingHandler,
            )
                .decodeFromJsonString()

            setState {
                organizations = organizationsFromBackend.toTypedArray()
            }
        }
    }

    private fun getProject(filterValue: ProjectFilters) {
        scope.launch {
            val projectsFromBackend: List<Project> = post(
                url = "$apiUrl/projects/not-deleted",
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
            changeUrl(state.selectedMenu, UserRatingTab, "#/${FrontendRoutes.CONTESTS_GLOBAL_RATING.path}",
                "#/${FrontendRoutes.CONTESTS_GLOBAL_RATING.path}")
            val href = window.location.href.substringBeforeLast("?")
            window.location.href = when (state.selectedMenu) {
                UserRatingTab.ORGS -> state.organizationFilters.name?.let { "$href?projectName=$it" } ?: href
                UserRatingTab.TOOLS -> state.projectFilters.name?.let { "$href?organizationName=$it" } ?: href
            }
        } else if (props.location != prevProps.location) {
            urlAnalysis(UserRatingTab, Role.NONE, false)
        }
    }

    override fun componentDidMount() {
        super.componentDidMount()
        val projectFilters = ProjectFilters(props.projectName)
        val organizationFilters = OrganizationFilters(props.organizationName)
        setState {
            this.projectFilters = projectFilters
            this.organizationFilters = organizationFilters
        }
        urlAnalysis(UserRatingTab, Role.NONE, false)
        getOrganization(organizationFilters)
        getProject(projectFilters)
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
                        state.organizations
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
