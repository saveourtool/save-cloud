@file:Suppress("FILE_WILDCARD_IMPORTS", "WildcardImport", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.utils.apiUrl
import com.saveourtool.save.frontend.utils.classLoadingHandler
import com.saveourtool.save.frontend.utils.decodeFromJsonString
import com.saveourtool.save.frontend.utils.get
import com.saveourtool.save.frontend.utils.unsafeMap
import com.saveourtool.save.info.UserInfo

import org.w3c.fetch.Headers
import react.*
import react.dom.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.td
import react.table.columns

/**
 * `Props` retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface ContestListViewProps : Props {
    var currentUserInfo: UserInfo?
}

/**
 * A view with collection of contests
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class ContestListView : AbstractView<ContestListViewProps, State>(false) {
    @Suppress("MAGIC_NUMBER")
    private val contestsTable = tableComponent(
        columns = columns<ContestDto> {
            column(id = "name", header = "Contest Name", { name }) { cellProps ->
                Fragment.create {
                    td {
                        a {
                            href = "#/contests/${cellProps.row.original.name}"
                            +cellProps.value
                        }
                    }
                }
            }
            column(id = "description", header = "Description", { description }) { cellProps ->
                Fragment.create {
                    td {
                        +(cellProps.value ?: "Description is not provided")
                    }
                }
            }
            column(id = "start_time", header = "Start Time", { startTime.toString() }) { cellProps ->
                Fragment.create {
                    td {
                        +cellProps.value.replace("T", " ")
                    }
                }
            }
            column(id = "end_time", header = "End Time", { endTime.toString() }) { cellProps ->
                Fragment.create {
                    td {
                        +cellProps.value.replace("T", " ")
                    }
                }
            }
        },
        initialPageSize = 10,
        useServerPaging = false,
        usePageSelection = false,
    )
    @Suppress(
        "EMPTY_BLOCK_STRUCTURE_ERROR",
        "TOO_LONG_FUNCTION",
        "MAGIC_NUMBER",
        "LongMethod",
    )
    override fun ChildrenBuilder.render() {
        contestsTable {
            getData = { _, _ ->
                val response = get(
                    url = "$apiUrl/contests/active",
                    headers = Headers().also {
                        it.set("Accept", "application/json")
                    },
                    loadingHandler = ::classLoadingHandler,
                )
                if (response.ok) {
                    response.unsafeMap {
                        it.decodeFromJsonString<List<ContestDto>>()
                    }
                        .toTypedArray()
                } else {
                    emptyArray()
                }
            }
        }
    }

    companion object : RStatics<ContestListViewProps, State, ContestListView, Context<RequestStatusContext>>(ContestListView::class) {
        init {
            contextType = requestStatusContext
        }
    }
}
