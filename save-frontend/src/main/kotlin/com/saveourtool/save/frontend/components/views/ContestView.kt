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
import csstype.ClassName

import org.w3c.fetch.Headers
import react.*
import react.dom.*

import kotlinx.html.js.onClickFunction
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.p

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
    private val contestInfo = contestInfoMenu()
    private val contestResults = contestResultsMenu()
    private val contestParticipants = contestParticipantsMenu()
    init {
        state.selectedMenu = ContestMenuBar.INFO
    }

    override fun ChildrenBuilder.render() {
        div {
            className = ClassName("d-flex justify-content-around")
            h1 {
                +"${props.currentContestName}"
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

    private fun ChildrenBuilder.renderResults() {
        contestResults {
            contestName = props.currentContestName ?: "UNDEFINED"
        }
    }

    private fun ChildrenBuilder.renderParticipants() {
        contestParticipants {
            contestName = props.currentContestName ?: "UNDEFINED"
        }
    }

    private fun ChildrenBuilder.renderInfo() {
        contestInfo {
            contestName = props.currentContestName ?: "UNDEFINED"
        }
    }

    private fun ChildrenBuilder.renderContestMenuBar() {
        div {
            className = ClassName("row align-items-center justify-content-center")
            nav {
                className = ClassName("nav nav-tabs mb-4")
                ContestMenuBar.values().forEachIndexed { i, contestMenu ->
                    li {
                        className = ClassName("nav-item")
                        val classVal =
                                if ((i == 0 && state.selectedMenu == null) || state.selectedMenu == contestMenu) " active font-weight-bold" else ""
                        p {
                            className = ClassName("nav-link $classVal text-gray-800")
                            onClick = {
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

    companion object : RStatics<ContestViewProps, ContestViewState, ContestView, Context<RequestStatusContext>>(ContestView::class) {
        init {
            contextType = requestStatusContext
        }
    }
}
