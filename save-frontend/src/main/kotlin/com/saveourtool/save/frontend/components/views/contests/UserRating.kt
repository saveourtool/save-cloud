/**
 * Card for the rendering of ratings: for organizations and tools
 */

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.frontend.externals.fontawesome.faArrowRight
import com.saveourtool.save.frontend.externals.fontawesome.faTrophy
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon

import csstype.*
import react.FC
import react.Props
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div

import kotlinx.js.jso

val userRatingFc = userRating()

/**
 * Enum that contains values for the tab that is used in rating card
 */
enum class UserRatingTab {
    ORGANIZATIONS, TOOLS
}

/**
 * properties for rating fc
 */
external interface UserRatingProps : Props {
    /**
     * string value of the selected tab: organization/tools/etc.
     */
    var selectedTab: String?

    /**
     * callback that will be passed into this fc from the view
     */
    var updateTabState: (String) -> Unit
}

/**
 * @return functional component for the rating card
 */
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
                    href = ""
                    +"View more "
                    fontAwesomeIcon(faArrowRight)
                }
            }
        }
    }
}
