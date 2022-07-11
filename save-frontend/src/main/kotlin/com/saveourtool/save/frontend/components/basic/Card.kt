/**
 * Components for cards
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.frontend.externals.fontawesome.FontAwesomeIconModule
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import csstype.ClassName
import react.FC

import react.PropsWithChildren
import react.dom.html.ReactHTML.div

/**
 * Props for card component
 */
external interface CardProps : PropsWithChildren {
    /**
     * font-awesome class to be used as an icon
     */
    var faIcon: FontAwesomeIconModule
}

/**
 * A functional `RComponent` for a card.
 *
 * @param isBordered adds a border to the card
 * @param hasBg adds a white background
 * @param isPaddingBottomNull disables bottom padding (pb-0)
 * @return a functional component representing a card
 */
@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
fun cardComponent(isBordered: Boolean = false, hasBg: Boolean = false, isPaddingBottomNull: Boolean = false) = FC<CardProps> { props ->
    val boarder = if (isBordered) "border-secondary" else ""
    val card = if (hasBg) "card" else ""
    val pb = if (isPaddingBottomNull) "pb-0" else ""
    div {
        className = ClassName("$card card-body mt-0 pt-0 pr-0 pl-0 $pb $boarder")
        div {
            className = ClassName("col mr-2 pr-0 pl-0")
            div {
                className = ClassName("mb-0 font-weight-bold text-gray-800")
                props.children?.let { +it }
            }
        }
        if (props.faIcon != undefined) {
            div {
                className = ClassName("col-auto")
                fontAwesomeIcon(icon = props.faIcon, classes = "fas fa-2x text-gray-300")
            }
        }
    }
}
