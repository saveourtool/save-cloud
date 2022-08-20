package com.saveourtool.save.frontend.components.views.contests

import csstype.ClassName
import csstype.rem
import kotlinx.js.jso
import react.FC
import react.Props
import react.dom.html.ReactHTML

enum class ContestTypesTab {
    ONGOING, FINISHED
}

val contestListFc = contestList()

external interface contestListProps : Props {
    var selectedTab: String?
    var updateTabState: Unit
}

fun contestList() = FC<contestListProps> { props ->
    ReactHTML.div {
        className = ClassName("col-lg-9")
        ReactHTML.div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                minHeight = 30.rem
            }

            ReactHTML.div {
                className = ClassName("col")

                title("Active Contests")
                tab(props.selectedTab, ContestTypesTab.values().map { it.name })
            }
        }
    }
}
