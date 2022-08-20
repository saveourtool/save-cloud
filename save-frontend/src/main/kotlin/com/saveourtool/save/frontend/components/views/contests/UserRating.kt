package com.saveourtool.save.frontend.components.views.contests


import csstype.*
import kotlinx.js.jso
import react.FC
import react.Props
import react.dom.html.ReactHTML.div


val userRatingFc = userRating()

enum class UserRatingTab {
    ORGANIZATIONS, TOOLS
}

external interface UserRatingProps : Props {
    var selectedTab: String?
    var updateTabState: Unit
}

fun userRating() = FC<UserRatingProps> { props ->
    div {
        className = ClassName("col-lg-3")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                minHeight = 30.rem
            }

            div {
                className = ClassName("col")

                title(" Global Rating")
                tab(props.selectedTab, UserRatingTab.values().map { it.name })
            }

        }
    }
}


