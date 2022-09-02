/**
 * A view with global rating
 */

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.Project
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.components.views.AbstractView
import com.saveourtool.save.frontend.externals.fontawesome.faTrophy
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.v1

import csstype.ClassName
import csstype.rem
import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.td
import react.table.columns

import kotlinx.coroutines.launch
import kotlinx.js.jso

/**
 * `Props` retrieved from router
 */
external interface ContestGlobalRatingProps : PropsWithChildren

/**
 * [State] of Contest Global Rating view component
 */
external interface ContestGlobalRatingViewState : State {
    /**
     * All organizations
     */
    var organizations: Array<Organization>

    /**
     * All projects
     */
    var projects: Array<Project>

    /**
     * Tab for selected organization or project tables
     */
    var selectedTab: UserRatingTab
}

/**
 * A Component for Contest Global Rating view
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class ContestGlobalRatingView : AbstractView<ContestGlobalRatingProps, ContestGlobalRatingViewState>(false) {
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
                                }
                                    ?: run {
                                        "img//company.svg"
                                    }
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
        isInvisibleTable = true,
        useServerPaging = false,
        usePageSelection = false,
        getAdditionalDependencies = {
            arrayOf(it)
        }
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
        isInvisibleTable = true,
        useServerPaging = false,
        usePageSelection = false,
        getAdditionalDependencies = {
            arrayOf(it)
        }
    )

    init {
        state.organizations = emptyArray()
        state.projects = emptyArray()
        state.selectedTab = UserRatingTab.ORGS
    }

    override fun componentDidMount() {
        super.componentDidMount()

        scope.launch {
            val organizationsFromBackend: List<Organization> = get(
                url = "$apiUrl/organizations/all",
                headers = jsonHeaders,
                loadingHandler = ::classLoadingHandler,
            )
                .decodeFromJsonString()

            val projectsFromBackend: List<Project> = get(
                url = "$apiUrl/projects/all",
                headers = jsonHeaders,
                loadingHandler = ::classLoadingHandler,
            )
                .decodeFromJsonString()

            setState {
                organizations = organizationsFromBackend.toTypedArray()
                projects = projectsFromBackend.toTypedArray()
            }
        }
    }

    override fun ChildrenBuilder.render() {
        div {
            className = ClassName("d-sm-flex align-items-center justify-content-center mb-4")
            h1 {
                className = ClassName("h3 mb-0 text-gray-800")
                title(" Global Rating", faTrophy)
            }
        }

        tab(state.selectedTab.name, UserRatingTab.values().map { it.name }) {
            setState {
                selectedTab = UserRatingTab.valueOf(it)
            }
        }
        when (state.selectedTab) {
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
