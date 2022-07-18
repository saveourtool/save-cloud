@file:Suppress("FILE_WILDCARD_IMPORTS", "WildcardImport", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.showContestEnrollerModal
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.classLoadingHandler
import com.saveourtool.save.info.UserInfo
import csstype.ClassName

import org.w3c.fetch.Headers
import react.*
import react.dom.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.td
import react.table.columns

/**
 * [Props] retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface ContestListViewProps : Props {
    var currentUserInfo: UserInfo?
}

/**
 * [State] of [ContestListView] component
 */
external interface ContestListViewState : State {
    /**
     * Flag to show project selector modal
     */
    var isProjectSelectorModalOpen: Boolean

    /**
     * Flag th show confirmation modal
     */
    var isConfirmationWindowOpen: Boolean

    /**
     * Name of a contest selected for enrollment
     */
    var selectedContestName: String?

    /**
     * Enrollment response received from backend
     */
    var enrollmentResponse: String?
}

/**
 * A view with collection of contests
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class ContestListView : AbstractView<ContestListViewProps, ContestListViewState>(false) {
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
            column(id = "enroll_button", header = "Registration", { name }) { cellProps ->
                Fragment.create {
                    td {
                        button {
                            className = ClassName("btn btn-primary")
                            onClick = {
                                setState {
                                    selectedContestName = cellProps.value
                                    isProjectSelectorModalOpen = true
                                }
                            }
                            +"Participate in ${cellProps.value}"
                        }
                    }
                }
            }
        },
        initialPageSize = 10,
        useServerPaging = false,
        usePageSelection = false,
    )
    init {
        state.selectedContestName = null
        state.isProjectSelectorModalOpen = false
        state.enrollmentResponse = null
        state.isConfirmationWindowOpen = false
    }
    @Suppress(
        "EMPTY_BLOCK_STRUCTURE_ERROR",
        "TOO_LONG_FUNCTION",
        "MAGIC_NUMBER",
        "LongMethod",
    )
    override fun ChildrenBuilder.render() {
        showContestEnrollerModal(
            state.isProjectSelectorModalOpen,
            state.selectedContestName,
            null,
            null,
            {
                setState {
                    enrollmentResponse = it
                    isConfirmationWindowOpen = true
                    isProjectSelectorModalOpen = false
                }
            }
        ) {
            setState { isProjectSelectorModalOpen = false }
        }
        runErrorModal(
            state.isConfirmationWindowOpen,
            "Contest Registration",
            state.enrollmentResponse ?: "",
            "Ok"
        ) {
            setState { isConfirmationWindowOpen = false }
        }
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

    companion object : RStatics<ContestListViewProps, ContestListViewState, ContestListView, Context<RequestStatusContext>>(ContestListView::class) {
        init {
            contextType = requestStatusContext
        }
    }
}
