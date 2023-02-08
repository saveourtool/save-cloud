@file:Suppress("FILE_WILDCARD_IMPORTS", "WildcardImport", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views.projectcollection

import com.saveourtool.save.entities.ProjectDto
import com.saveourtool.save.filters.ProjectFilters
import com.saveourtool.save.frontend.TabMenuBar
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.components.tables.TableProps
import com.saveourtool.save.frontend.components.tables.columns
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.components.tables.value
import com.saveourtool.save.frontend.components.views.AbstractView
import com.saveourtool.save.frontend.components.views.contests.tab
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.classLoadingHandler
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes

import csstype.ClassName
import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.td

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Enum that contains values for project
 */
enum class ProjectTab {
    PRIVATE,
    PUBLIC,
    ;

    companion object : TabMenuBar<ProjectTab> {
        // The string is the postfix of a [regexForUrlClassification] for parsing the url
        private val postfixInRegex = values().joinToString("|") { it.name.lowercase() }
        override val nameOfTheHeadUrlSection = ""
        override val defaultTab: ProjectTab = PUBLIC
        override val regexForUrlClassification = Regex("/${FrontendRoutes.PROJECTS.path}/($postfixInRegex)")
        override fun valueOf(elem: String): ProjectTab = ProjectTab.valueOf(elem)
        override fun values(): Array<ProjectTab> = ProjectTab.values()
    }
}

/**
 * `Props` retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface CreationViewProps : Props {
    var currentUserInfo: UserInfo?
}

/**
 * [State] of Collection view component
 */
external interface CollectionViewState : State, HasSelectedMenu<ProjectTab>

/**
 * A view with collection of projects
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
@Suppress(
    "TYPE_ALIAS",
    "MAGIC_NUMBER",
)
class CollectionView : AbstractView<CreationViewProps, CollectionViewState>() {
    private val publicProjectsTable: FC<TableProps<ProjectDto>> = tableComponent(
        columns = {
            columns {
                column(id = "organization", header = "Organization", { organizationName }) { cellContext ->
                    Fragment.create {
                        td {
                            a {
                                href = "#/${cellContext.row.original.organizationName}"
                                +cellContext.value
                            }
                        }
                    }
                }
                column(id = "name", header = "Evaluated Tool", { name }) { cellContext ->
                    Fragment.create {
                        td {
                            a {
                                href = "#/${cellContext.row.original.organizationName}/${cellContext.value}"
                                +cellContext.value
                            }
                            privacySpan(cellContext.row.original)
                        }
                    }
                }
                column(id = "passed", header = "Description") { cellContext ->
                    Fragment.create {
                        td {
                            +(cellContext.value.description.ifEmpty { "Description not provided" })
                        }
                    }
                }
                column(id = "rating", header = "Contest Rating") {
                    Fragment.create {
                        td {
                            +"0"
                        }
                    }
                }
            }
        },
        isTransparentGrid = true,
        initialPageSize = 10,
        useServerPaging = false,
        usePageSelection = false,
    )
    private val privateProjectsTable: FC<TableProps<ProjectDto>> = tableComponent(
        columns = {
            columns {
                column(id = "organization", header = "Organization", { organizationName }) { cellContext ->
                    Fragment.create {
                        td {
                            a {
                                href = "#/${cellContext.row.original.organizationName}"
                                +cellContext.value
                            }
                        }
                    }
                }
                column(id = "name", header = "Evaluated Tool", { name }) { cellContext ->
                    Fragment.create {
                        td {
                            a {
                                href = "#/${cellContext.row.original.organizationName}/${cellContext.value}"
                                +cellContext.value
                            }
                            privacySpan(cellContext.row.original)
                        }
                    }
                }
                column(id = "passed", header = "Description") { cellContext ->
                    Fragment.create {
                        td {
                            +(cellContext.value.description.ifEmpty { "Description not provided" })
                        }
                    }
                }
                column(id = "rating", header = "Contest Rating") {
                    Fragment.create {
                        td {
                            +"0"
                        }
                    }
                }
            }
        },
        isTransparentGrid = true,
        initialPageSize = 10,
        useServerPaging = false,
        usePageSelection = false,
    )

    init {
        state.selectedMenu = ProjectTab.defaultTab
    }

    @Suppress(
        "EMPTY_BLOCK_STRUCTURE_ERROR",
        "TOO_LONG_FUNCTION",
        "MAGIC_NUMBER",
        "LongMethod",
    )
    override fun ChildrenBuilder.render() {
        div {
            className = ClassName("row justify-content-center")
            div {
                className = ClassName("col-lg-10 mt-4 min-vh-100")
                div {
                    className = ClassName("row mb-2")
                    topLeftCard()
                    topRightCard()
                }

                div {
                    className = ClassName("card flex-md-row")
                    div {
                        className = ClassName("col")

                        tab(state.selectedMenu.name, ProjectTab.values().map { it.name }, "nav nav-tabs mt-3") {
                            setState {
                                selectedMenu = ProjectTab.valueOf(it)
                            }
                        }

                        when (state.selectedMenu) {
                            ProjectTab.PUBLIC -> publicProjectsTable {
                                getData = { _, _ ->
                                    val response = post(
                                        url = "$apiUrl/projects/by-filters",
                                        headers = jsonHeaders,
                                        body = Json.encodeToString(ProjectFilters(name = "", public = true)),
                                        loadingHandler = ::classLoadingHandler,
                                        responseHandler = ::noopResponseHandler
                                    )
                                    if (response.ok) {
                                        response.unsafeMap {
                                            it.decodeFromJsonString<Array<ProjectDto>>()
                                        }
                                    } else {
                                        emptyArray()
                                    }
                                }
                            }

                            ProjectTab.PRIVATE -> privateProjectsTable {
                                getData = { _, _ ->
                                    val response = post(
                                        url = "$apiUrl/projects/by-filters",
                                        headers = jsonHeaders,
                                        body = Json.encodeToString(ProjectFilters(name = "", public = false)),
                                        loadingHandler = ::classLoadingHandler,
                                        responseHandler = ::noopResponseHandler
                                    )
                                    if (response.ok) {
                                        response.unsafeMap {
                                            it.decodeFromJsonString<Array<ProjectDto>>()
                                        }
                                    } else {
                                        emptyArray()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object : RStatics<CreationViewProps, CollectionViewState, CollectionView, Context<RequestStatusContext>>(CollectionView::class) {
        init {
            contextType = requestStatusContext
        }
    }
}
