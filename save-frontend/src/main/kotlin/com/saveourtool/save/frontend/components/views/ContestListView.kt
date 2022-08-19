@file:Suppress("FILE_WILDCARD_IMPORTS", "WildcardImport", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.ContestNameProps
import com.saveourtool.save.frontend.components.basic.showContestEnrollerModal
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.classLoadingHandler
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes

import org.w3c.fetch.Headers
import react.*
import react.dom.html.ReactHTML.a
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
    private val openParticipateModal: (String) -> Unit = { contestName ->
        setState {
            selectedContestName = contestName
            isProjectSelectorModalOpen = true
        }
    }

    @Suppress("MAGIC_NUMBER")
    private val contestsTable = tableComponent(
        columns = columns<ContestDto> {
            column(id = "name", header = "Contest Name", { this }) { cellProps ->
                Fragment.create {
                    td {
                        onClick = {
                            openParticipateModal(cellProps.value.name)
                        }
                        a {
                            href = "#/${FrontendRoutes.CONTESTS.path}/${cellProps.row.original.name}"
                            +cellProps.value.name
                        }
                    }
                }
            }
            column(id = "description", header = "Description", { this }) { cellProps ->
                Fragment.create {
                    td {
                        onClick = {
                            openParticipateModal(cellProps.value.name)
                        }
                        +(cellProps.value.description ?: "Description is not provided")
                    }
                }
            }
            column(id = "start_time", header = "Start Time", { this }) { cellProps ->
                Fragment.create {
                    td {
                        onClick = {
                            openParticipateModal(cellProps.value.name)
                        }
                        +cellProps.value.startTime.toString().replace("T", " ")
                    }
                }
            }
            column(id = "end_time", header = "End Time", { this }) { cellProps ->
                Fragment.create {
                    td {
                        onClick = {
                            openParticipateModal(cellProps.value.name)
                        }
                        +cellProps.value.endTime.toString().replace("T", " ")
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
            ContestNameProps(state.selectedContestName ?: ""),
            { setState { isProjectSelectorModalOpen = false } }
        ) {
            setState {
                enrollmentResponse = it
                isConfirmationWindowOpen = true
                isProjectSelectorModalOpen = false
            }
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
