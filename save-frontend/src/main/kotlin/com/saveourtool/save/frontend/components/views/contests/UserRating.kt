package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.frontend.externals.fontawesome.faArrowRight
import com.saveourtool.save.frontend.externals.fontawesome.faTrophy
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import csstype.*
import kotlinx.js.jso
import react.FC
import react.Props
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div

val userRatingFc = userRating()

enum class UserRatingTab {
    ORGANIZATIONS, TOOLS
}

external interface UserRatingProps : Props {
    var selectedTab: String?
    var updateTabState: (String) -> Unit
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
                title(" Global Rating", faTrophy)
                tab(props.selectedTab, UserRatingTab.values().map { it.name }, props.updateTabState)
                // FixMe: user rating here
                a {
                    // FixMe: new view on this link
                    href =""
                    +"View more "
                    fontAwesomeIcon(faArrowRight)
                }
            }
        }
    }
}
