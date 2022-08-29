/**
 * Contests "market" - showcase for users, where they can navigate and check contests
 */

@file:Suppress("FILE_WILDCARD_IMPORTS", "WildcardImport", "MAGIC_NUMBER")

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.Project
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.ContestNameProps
import com.saveourtool.save.frontend.components.basic.showContestEnrollerModal
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.components.views.AbstractView
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.classLoadingHandler
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.LocalDateTime
import com.saveourtool.save.utils.getCurrentLocalDateTime

import csstype.ClassName
import csstype.rem
import org.w3c.fetch.Headers
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.main

import kotlinx.coroutines.launch
import kotlinx.js.jso

/**
 * TODO:
 * 1. Хотите создавать контесты - напишите нам
 * 2. Добавить свой контест: выбираем организацию и дальше добавляем
 * 3. Гранд чемпионы SAVE
 * 4. Обратный отсчет
 * 5. Количество контествов и участников
 */

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
     * list of contests that are not expired yet (deadline is not reached)
     */
    var activeContests: Set<ContestDto>

    /**
     * list of contests that are expired  (deadline has reached)
     */
    var finishedContests: Set<ContestDto>

    /**
     * all organizations registered in SAVE
     */
    var organizations: Set<Organization>

    /**
     * all public projects registered in SAVE
     */
    var projects: Set<Project>

    /**
     * current time
     */
    var currentDateTime: LocalDateTime

    /**
     * selected tab
     */
    var selectedContestsTab: String?

    /**
     * selected tab
     */
    var selectedRatingTab: String?

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
    private val openParticipateModal: (String) -> Unit = { contestName ->
        setState {
            selectedContestName = contestName
            isProjectSelectorModalOpen = true
        }
    }

    init {
        state.selectedRatingTab = UserRatingTab.ORGANIZATIONS.name
        state.selectedContestsTab = ContestTypesTab.ACTIVE.name
        state.finishedContests = emptySet()
        state.activeContests = emptySet()
        state.organizations = emptySet()
        state.currentDateTime = getCurrentLocalDateTime()
    }

    override fun componentDidMount() {
        super.componentDidMount()
        scope.launch {
            getAndInitActiveContests()
            getAndInitFinishedContests()
            // FixMe: in future here will only be TOP tools (for example 10)
            getAndInitOrganizations()
            getAndInitProjects()
        }
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod")
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

        main {
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

                            proposeContest()
                        }

                        div {
                            className = ClassName("row mb-2")
                            userRatingFc {
                                selectedTab = state.selectedRatingTab
                                updateTabState = { setState { selectedRatingTab = it } }
                                organizations = state.organizations
                                projects = state.projects
                            }

                            contestListFc {
                                activeContests = state.activeContests
                                finishedContests = state.finishedContests
                                selectedTab = state.selectedContestsTab
                                updateTabState = { setState { selectedContestsTab = it } }
                                updateSelectedContestName = { setState { selectedContestName = it } }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun getAndInitProjects() {
        val response = get(
            url = "$apiUrl/projects/all",
            headers = Headers().also {
                it.set("Accept", "application/json")
            },
            loadingHandler = ::classLoadingHandler,
        )
        val update = if (response.ok) {
            response.unsafeMap {
                it.decodeFromJsonString<List<Project>>()
            }
                .toTypedArray()
        } else {
            emptyArray()
        }.toSet()

        setState {
            projects = update
        }
    }

    /**
     * unfortunately we cannot use generic function here, so it is easier to duplicate the code
     */
    private suspend fun getAndInitOrganizations() {
        val response = get(
            url = "$apiUrl/organizations/all",
            headers = Headers().also {
                it.set("Accept", "application/json")
            },
            loadingHandler = ::classLoadingHandler,
        )
        val update = if (response.ok) {
            response.unsafeMap {
                it.decodeFromJsonString<List<Organization>>()
            }
                .toTypedArray()
        } else {
            emptyArray()
        }.toSet()

        setState {
            organizations = update
        }
    }

    private suspend fun getAndInitFinishedContests() {
        getAndUpdateContestsState("$apiUrl/contests/finished") {
            setState {
                finishedContests = it
            }
        }
    }

    private suspend fun getAndInitActiveContests() {
        getAndUpdateContestsState("$apiUrl/contests/active") {
            setState {
                activeContests = it
            }
        }
    }

    private suspend fun getAndUpdateContestsState(url: String, setState: (Set<ContestDto>) -> Unit) {
        val response = get(
            url = url,
            headers = Headers().also {
                it.set("Accept", "application/json")
            },
            loadingHandler = ::classLoadingHandler,
        )
        val update = if (response.ok) {
            response.unsafeMap {
                it.decodeFromJsonString<List<ContestDto>>()
            }
                .toTypedArray()
        } else {
            emptyArray()
        }.toSet()

        setState(update)
    }

    companion object :
        RStatics<ContestListViewProps, ContestListViewState, ContestListView, Context<RequestStatusContext>>(
        ContestListView::class
    ) {
        init {
            contextType = requestStatusContext
        }
    }
}
