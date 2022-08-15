@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.contests

import csstype.*
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h6

import kotlinx.js.jso

/**
 * RESULTS tab in ContestView
 */
val contestResultsMenu = contestResultsMenu()

/**
 * ContestResultsMenu component props
 */
external interface ContestResultsMenuProps : Props {
    /**
     * Name of a current contest
     */
    var contestName: String
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "MAGIC_NUMBER",
    "AVOID_NULL_CHECKS"
)
private fun contestResultsMenu(
) = FC<ContestResultsMenuProps> {
    div {
        className = ClassName("mb-3")
        style = jso {
            justifyContent = JustifyContent.center
            display = Display.flex
            flexDirection = FlexDirection.column
            alignItems = AlignItems.center
        }
        h6 {
            className = ClassName("text-center")
            +"You didn't submit your tool yet."
        }
    }
}
