package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.frontend.externals.fontawesome.faArrowRight
import com.saveourtool.save.frontend.externals.fontawesome.faCode
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.validation.FrontendRoutes
import csstype.ClassName
import csstype.rem
import kotlinx.js.jso
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.html.ButtonType
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.button

enum class ContestTypesTab {
    ACTIVE, FINISHED, PLANNED
}

val contestListFc = contestList()

external interface ContestListProps : Props {
    var selectedTab: String?
    var updateTabState: (String) -> Unit
    var finishedContests: Set<ContestDto>
    var activeContests: Set<ContestDto>
    var updateSelectedContestName: (String) -> Unit
}

fun contestList() = FC<ContestListProps> { props ->
    ReactHTML.div {
        className = ClassName("col-lg-9")
        ReactHTML.div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                minHeight = 30.rem
            }

            ReactHTML.div {
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

private fun ChildrenBuilder.contests(props: ContestListProps) {
    when (props.selectedTab) {
        ContestTypesTab.ACTIVE.name -> contestListTable(props.activeContests, props.updateSelectedContestName)
        ContestTypesTab.FINISHED.name -> contestListTable(props.finishedContests, props.updateSelectedContestName)
        ContestTypesTab.PLANNED.name -> {
            // FixMe: Add planned contests
        }
    }
}

private fun ChildrenBuilder.contestListTable(contests: Set<ContestDto>, updateSelectedContestName: (String) -> Unit) {
    contests.forEachIndexed { i, contest ->
            ReactHTML.div {
                className = ClassName("media text-muted pb-3")
                ReactHTML.img {
                    className = ClassName("rounded")
                    asDynamic()["data-src"] =
                        "holder.js/32x32?theme=thumb&amp;bg=007bff&amp;fg=007bff&amp;size=1"
                    src = "img/undraw_code_inspection_bdl7.svg"
                    asDynamic()["data-holder-rendered"] = "true"
                    style = jso {
                        width = 4.2.rem
                    }
                }

                ReactHTML.p {
                    className = ClassName("media-body pb-3 mb-0 small lh-125 border-bottom border-gray text-left")
                    ReactHTML.strong {
                        className = ClassName("d-block text-gray-dark")
                        +contest.name
                    }
                    +(contest.description ?: "")

                    ReactHTML.div {
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
                        ReactHTML.a {
                            className = ClassName("btn btn-outline-info mr-2")
                            href = "#/${ FrontendRoutes.CONTESTS.path}/${contest.name}"
                            +"Rules and more "
                            fontAwesomeIcon(icon = faArrowRight)
                        }
                    }
                }
        }
    }
}
