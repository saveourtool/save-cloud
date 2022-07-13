@file:Suppress("FILE_WILDCARD_IMPORTS", "WildcardImport", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.contests.contestInfoMenu
import com.saveourtool.save.frontend.components.basic.contests.contestParticipantsMenu
import com.saveourtool.save.frontend.components.basic.contests.contestResultsMenu
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.classLoadingHandler
import com.saveourtool.save.info.UserInfo

import org.w3c.fetch.Headers
import react.*
import react.dom.*

import kotlinx.html.js.onClickFunction

/**
 * Enum that defines the bar that is chosen
 */
enum class ContestMenuBar {
    INFO,
    PARTICIPANTS,
    RESULTS,
    ;
}

/**
 * `Props` retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface ContestViewProps : Props {
    var currentUserInfo: UserInfo?
    var currentContestName: String?
}

/**
 * [State] for [ContestView]
 */
external interface ContestViewState : State {
    /**
     * Current selected menu
     */
    var selectedMenu: ContestMenuBar?
}

/**
 * A view with collection of projects
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class ContestView : AbstractView<ContestViewProps, ContestViewState>(false) {
    init {
        state.selectedMenu = ContestMenuBar.INFO
    }

    override fun RBuilder.render() {
        div("d-flex justify-content-around") {
            h1 {
                attrs.text("${props.currentContestName}")
            }
        }
        renderContestMenuBar()

        when (state.selectedMenu) {
            ContestMenuBar.INFO -> renderInfo()
            ContestMenuBar.RESULTS -> renderResults()
            ContestMenuBar.PARTICIPANTS -> renderParticipants()
            else -> throw NotImplementedError()
        }
    }

    private fun RBuilder.renderResults() {
        contestResultsMenu {
            attrs.contestName = props.currentContestName ?: "UNDEFINED"
        }
    }

    private fun RBuilder.renderParticipants() {
        contestParticipantsMenu {
            attrs.contestName = props.currentContestName ?: "UNDEFINED"
        }
    }

    private fun RBuilder.renderInfo() {
        contestInfoMenu {
            attrs.contestName = props.currentContestName ?: "UNDEFINED"
        }
    }

    private fun RBuilder.renderContestMenuBar() {
        div("row align-items-center justify-content-center") {
            nav("nav nav-tabs mb-4") {
                ContestMenuBar.values().forEachIndexed { i, contestMenu ->
                    li("nav-item") {
                        val classVal =
                                if ((i == 0 && state.selectedMenu == null) || state.selectedMenu == contestMenu) " active font-weight-bold" else ""
                        p("nav-link $classVal text-gray-800") {
                            attrs.onClickFunction = {
                                if (state.selectedMenu != contestMenu) {
                                    setState {
                                        selectedMenu = contestMenu
                                    }
                                }
                            }
                            +contestMenu.name
                        }
                    }
                }
            }
        }
    }

    private suspend fun ComponentWithScope<*, *>.getContest(contestName: String) = get(
        "$apiUrl/contests/$contestName",
        Headers().apply {
            set("Accept", "application/json")
        },
        loadingHandler = ::classLoadingHandler,
    )
        .unsafeMap {
            it.decodeFromJsonString<ContestDto>()
        }

    companion object : RStatics<ContestViewProps, ContestViewState, ContestView, Context<RequestStatusContext>>(ContestView::class) {
        init {
            contextType = requestStatusContext
        }
    }
}
