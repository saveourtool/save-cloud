@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views.toprating

import com.saveourtool.save.entities.OrganizationDto
import com.saveourtool.save.filters.OrganizationFilter
import com.saveourtool.save.frontend.components.basic.table.filters.nameFiltersRow
import com.saveourtool.save.frontend.components.tables.*
import com.saveourtool.save.frontend.externals.fontawesome.faTrophy
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.frontend.utils.noopResponseHandler
import com.saveourtool.save.v1

import js.core.jso
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.tr
import react.router.dom.Link
import web.cssom.*

import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val organizationRatingTab: FC<Props> = FC { _ ->

    val (organizationFilter, setOrganizationFilter) = useState(OrganizationFilter.created)

    @Suppress(
        "TOO_MANY_LINES_IN_LAMBDA",
        "MAGIC_NUMBER",
        "TYPE_ALIAS",
    )
    val tableWithOrganizationRating: FC<TableProps<OrganizationDto>> = tableComponent(
        columns = {
            columns {
                column(id = "index", header = "Position") { cellContext ->
                    Fragment.create {
                        td {
                            val index = cellContext.row.index + 1 + cellContext.pageIndex * cellContext.pageSize
                            var isTrophy = false
                            var newColor = ""
                            when (index) {
                                1 -> {
                                    isTrophy = true
                                    newColor = "#ebcc36"
                                }
                                2 -> {
                                    isTrophy = true
                                    newColor = "#7d7d7d"
                                }
                                3 -> {
                                    isTrophy = true
                                    newColor = "#a15703"
                                }
                            }
                            if (isTrophy) {
                                style = jso {
                                    color = newColor.unsafeCast<Color>()
                                }
                                fontAwesomeIcon(icon = faTrophy)
                            }
                            +" $index"
                        }
                    }
                }
                column(id = "name", header = "Name", { name }) { cellContext ->
                    Fragment.create {
                        td {
                            Link {
                                img {
                                    className =
                                            ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
                                    src = cellContext.row.original.avatar?.let {
                                        "/api/$v1/avatar$it"
                                    } ?: "img/company.svg"
                                    style = jso {
                                        height = 2.rem
                                        width = 2.rem
                                    }
                                }
                                to = "#/${cellContext.value}"
                                +" ${cellContext.value}"
                            }
                        }
                    }
                }
                column(id = "rating", header = "Rating") { cellContext ->
                    Fragment.create {
                        td {
                            +cellContext.value.rating.toString()
                        }
                    }
                }
            }
        },
        useServerPaging = false,
        isTransparentGrid = true,
        commonHeader = { tableInstance, _ ->
            tr {
                th {
                    colSpan = tableInstance.visibleColumnsCount()
                    nameFiltersRow {
                        name = organizationFilter.prefix
                        onChangeFilters = { filterValue ->
                            val filter = if (filterValue.isNullOrEmpty()) {
                                OrganizationFilter.created
                            } else {
                                OrganizationFilter(filterValue)
                            }
                            setOrganizationFilter(filter)
                            window.location.href = buildString {
                                append(window.location.href.substringBefore("?"))
                                filterValue?.let { append("?organizationName=$filterValue") }
                            }
                        }
                    }
                }
            }
        }
    ) {
        arrayOf(it)
    }

    div {
        className = ClassName("row justify-content-center")
        div {
            className = ClassName("col-8")
            tableWithOrganizationRating {
                getData = { _, _ ->
                    post(
                        url = "$apiUrl/organizations/all-by-filters",
                        headers = jsonHeaders,
                        body = Json.encodeToString(organizationFilter),
                        loadingHandler = ::noopLoadingHandler,
                        responseHandler = ::noopResponseHandler,
                    )
                        .decodeFromJsonString<Array<OrganizationDto>>().let { array ->
                            array.sortByDescending { it.rating }
                            array
                        }
                }
            }
        }
    }
}
