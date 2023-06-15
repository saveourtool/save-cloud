package com.saveourtool.save.frontend.components.views.toprating

import com.saveourtool.save.frontend.TabMenuBar
import com.saveourtool.save.frontend.components.views.contests.tab
import com.saveourtool.save.frontend.components.views.contests.title
import com.saveourtool.save.frontend.externals.fontawesome.faTrophy
import com.saveourtool.save.frontend.utils.Style
import com.saveourtool.save.frontend.utils.useBackground
import com.saveourtool.save.validation.FrontendRoutes
import react.VFC
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.useState
import web.cssom.ClassName

val topRatingView = VFC {
    useBackground(Style.WHITE)

    val (selectedMenu, setSelectedMenu) = useState(TopRatingTab.USERS)

    div {
        className = ClassName("d-sm-flex align-items-center justify-content-center mb-4")
        h1 {
            className = ClassName("h3 mb-0 text-gray-800")
            title(" Top Rating", faTrophy)
        }
    }

    tab(selectedMenu.name, TopRatingTab.values().map { it.name }, "nav nav-tabs mt-3") { value ->
        setSelectedMenu(TopRatingTab.valueOf(value))
    }

    when (selectedMenu) {
        TopRatingTab.USERS -> renderUserRatingTable()
        TopRatingTab.ORGANIZATIONS -> div {}
    }

}

/**
 * Enum that contains values for the tab that is used in top rating view
 */
enum class TopRatingTab {
    USERS,
    ORGANIZATIONS,
    ;

    companion object : TabMenuBar<TopRatingTab> {
        override val nameOfTheHeadUrlSection = ""
        override val defaultTab: TopRatingTab = TopRatingTab.USERS
        override val regexForUrlClassification = "/${FrontendRoutes.TOP_RATING.path}"
        override fun valueOf(elem: String): TopRatingTab = TopRatingTab.valueOf(elem)
        override fun values(): Array<TopRatingTab> = TopRatingTab.values()
    }
}