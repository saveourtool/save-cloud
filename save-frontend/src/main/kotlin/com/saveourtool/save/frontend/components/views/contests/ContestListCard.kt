/**
 * a card with contests list from ContestListView
 */

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.frontend.externals.fontawesome.faArrowRight
import com.saveourtool.save.frontend.externals.fontawesome.faCode
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.validation.FrontendRoutes

import csstype.ClassName
import csstype.rem
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.strong

import kotlinx.js.jso

val contestListFc = contestList()

/**
 * this enum is used in a tab for contests
 */
enum class ContestTypesTab {
    ACTIVE, FINISHED, PLANNED
}

/**
 * properties for contestList FC
 */
external interface ContestListProps : Props {
    /**
     * contest tab selected by user (propagated from state)
     */
    var selectedTab: String?

    /**
     * callback to update state with a tab selection
     */
    var updateTabState: (String) -> Unit

    /**
     * all finished contests
     */
    var finishedContests: Set<ContestDto>

    /**
     * all active contests
     */
    var activeContests: Set<ContestDto>

    /**
     * callback to set selected contest (will trigger a modal window with a participation modal window)
     */
    var updateSelectedContestName: (String) -> Unit
}

private fun ChildrenBuilder.contests(props: ContestListProps) {
    when (props.selectedTab) {
        ContestTypesTab.ACTIVE.name -> contestListTable(props.activeContests, props.updateSelectedContestName)
        ContestTypesTab.FINISHED.name -> contestListTable(props.finishedContests, props.updateSelectedContestName)
        ContestTypesTab.PLANNED.name -> {
            // FixMe: Add planned contests
        }
    }
}

@Suppress("MAGIC_NUMBER")
private fun ChildrenBuilder.contestListTable(contests: Set<ContestDto>, updateSelectedContestName: (String) -> Unit) {
    contests.forEachIndexed { i, contest ->
        div {
            className = ClassName("media text-muted pb-3")
            img {
                className = ClassName("rounded")
                src = "img/undraw_code_inspection_bdl7.svg"
                style = jso {
                    width = 4.2.rem
                }
            }

            p {
                className = ClassName("media-body pb-3 mb-0 small lh-125 border-bottom border-gray text-left")
                strong {
                    className = ClassName("d-block text-gray-dark")
                    +contest.name
                }
                +(contest.description ?: "")

                div {
                    className = ClassName("navbar-landing mt-3")
                    button {
                        type = ButtonType.button
                        className = ClassName("btn btn-outline-success ml-auto mr-2")
                        onClick = {
                            // FixMe: fix the selector here
                            updateSelectedContestName(contest.name)
                        }
                        +"Enroll"
                    }
                    a {
                        className = ClassName("btn btn-outline-info mr-2")
                        href = "#/${FrontendRoutes.CONTESTS.path}/${contest.name}"
                        +"Rules and more "
                        fontAwesomeIcon(icon = faArrowRight)
                    }
                }
            }
        }
    }
}

/**
 * @return functional component that render the stylish table with contests
 */
fun contestList() = FC<ContestListProps> { props ->
    div {
        className = ClassName("col-lg-9")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                minHeight = 30.rem
            }

            div {
                className = ClassName("col")

                title(" Available Contests", faCode)

                tab(
                    props.selectedTab,
                    ContestTypesTab.values().map { it.name },
                    props.updateTabState
                )

                contests(props)
            }
        }
    }
}
