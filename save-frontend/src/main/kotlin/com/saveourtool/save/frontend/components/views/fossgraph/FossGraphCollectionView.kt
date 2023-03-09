@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.fossgraph

import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.filters.VulnerabilityFilter
import com.saveourtool.save.frontend.components.basic.nameFiltersRow
import com.saveourtool.save.frontend.components.tables.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopResponseHandler
import com.saveourtool.save.validation.FrontendRoutes

import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.tr
import react.router.dom.Link
import react.router.useNavigate

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * [VFC] for fossGraphCollection view
 */
val fossGraphCollectionView: VFC = VFC {
    useBackground(Style.WHITE)
    val navigate = useNavigate()

    val (vulnerabilityFilters, setVulnerabilityFilters) = useState(VulnerabilityFilter.created)

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
                                VulnerabilityFilter.created
                            } else {
                                VulnerabilityFilter(filterValue)
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

    div {
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

/**
 * `Props` for vulnerabilities table
 */
external interface FiltersProps : TableProps<VulnerabilityDto> {
    /**
     * All filters in one value [filters]
     */
    var filters: VulnerabilityFilter?
}
