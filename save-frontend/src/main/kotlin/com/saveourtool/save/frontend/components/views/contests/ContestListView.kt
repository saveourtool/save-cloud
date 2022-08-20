@file:Suppress("FILE_WILDCARD_IMPORTS", "WildcardImport", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.frontend.components.views.AbstractView
import com.saveourtool.save.frontend.utils.apiUrl
import com.saveourtool.save.frontend.utils.decodeFromJsonString
import com.saveourtool.save.frontend.utils.get
import com.saveourtool.save.frontend.utils.unsafeMap
import com.saveourtool.save.info.UserInfo
import csstype.ClassName
import kotlinx.coroutines.launch
import org.w3c.fetch.Headers
import com.saveourtool.save.frontend.utils.classLoadingHandler
import csstype.rem
import kotlinx.js.jso


import react.*
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div

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
    var contests: Array<ContestDto>

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
class ContestListView : AbstractView<ContestListViewProps, ContestListViewState>() {
    init {
        state.contests = emptyArray()
    }

    override fun componentDidMount() {
        super.componentDidMount()
        scope.launch {
            getAndInitContests()
        }
    }

/*    private val openParticipateModal: (String) -> Unit = { contestName ->
        setState {
            selectedContestName = contestName
            isProjectSelectorModalOpen = true
        }
    }*/

    override fun ChildrenBuilder.render() {
        ReactHTML.main {
            className = ClassName("main-content mt-0 ps")
            div {
                className = ClassName("page-header align-items-start min-vh-100")
                div {
                    className = ClassName("row justify-content-center")
                    div {
                        className = ClassName("col-lg-9")
                        div {
                            className = ClassName("row mb-2")
                            featuredContest()
                            newContestsCard()

                            countDownFc {
                                contests = state.contests
                            }
                        }

                        div {
                            className = ClassName("row mb-2")
                            div {
                                className = ClassName("col-lg-5")
                                div {
                                    className = ClassName("card flex-md-row mb-1 box-shadow")
                                    style = jso {
                                        minHeight = 7.rem
                                    }
                                }
                            }
                            div {
                                className = ClassName("col-lg-5")
                                div {
                                    className = ClassName("card flex-md-row mb-1 box-shadow")
                                    style = jso {
                                        minHeight = 7.rem
                                    }
                                }
                            }
                            div {
                                className = ClassName("col-lg-2")
                                div {
                                    className = ClassName("card flex-md-row mb-1 box-shadow")
                                    style = jso {
                                        minHeight = 7.rem
                                    }
                                }
                            }
                        }

                        div {
                            className = ClassName("row mb-2")
                            userRatingFc {}
                            contestListFc {}
                        }
                    }


                    /*div {
                        className = ClassName("col-lg-4 mb-4")
                        div {
                            className = ClassName("card shadow mb-4")

                        }
                        div {
                            className = ClassName("card shadow mb-4")

                        }
                    }*/
                }
            }
        }
    }

    /*   @Suppress("MAGIC_NUMBER")
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
       }*/

    private suspend fun getAndInitContests() {
        val response = get(
            url = "$apiUrl/contests/active",
            headers = Headers().also {
                it.set("Accept", "application/json")
            },
            loadingHandler = ::classLoadingHandler,
        )
        val contestsList = if (response.ok) {
            response.unsafeMap {
                it.decodeFromJsonString<List<ContestDto>>()
            }
                .toTypedArray()
        } else {
            emptyArray()
        }

        setState { contests = contestsList }
    }


/*       companion object : RStatics<ContestListViewProps, ContestListViewState, ContestListView, Context<RequestStatusContext>>(
           ContestListView::class) {
           init {
               contextType = requestStatusContext
           }
       }*/
}
