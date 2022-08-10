@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.contests

import csstype.ClassName
import react.*
import react.dom.html.ReactHTML.h6

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
    h6 {
        className = ClassName("text-center")
        +"You didn't submit your tool yet."
    }
}
