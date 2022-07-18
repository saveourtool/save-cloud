@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.contests

import react.*

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
    +"Your results will be here"
}
