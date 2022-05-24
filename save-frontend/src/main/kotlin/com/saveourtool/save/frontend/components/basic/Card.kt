/**
 * Components for cards
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon

import react.PropsWithChildren
import react.dom.RDOMBuilder
import react.dom.div
import react.fc

import kotlinx.html.DIV

/**
 * [RProps] for card component
 */
external interface CardProps : PropsWithChildren {
    /**
     * font-awesome class to be used as an icon
     */
    var faIcon: String
}

/**
 * A functional `RComponent` for a card.
 *
 * @param contentBuilder a builder function for card content
 * @param isBordered - adds a border to the card
 * @param hasBg - adds a white background
 * @return a functional component representing a card
 */
@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
fun cardComponent(isBordered: Boolean = false, hasBg: Boolean = false, contentBuilder: RDOMBuilder<DIV>.() -> Unit) = fc<CardProps> { props ->
    val boarder = if (isBordered) "border-secondary" else ""
    val card = if (hasBg) "card" else ""
    div("$card card-body mt-0 pt-0 pr-0 pl-0 $boarder") {
        div("col mr-2 pr-0 pl-0") {
            div("mb-0 font-weight-bold text-gray-800") {
                contentBuilder.invoke(this)
            }
        }
        if (props.faIcon != undefined) {
            div("col-auto") {
                fontAwesomeIcon(icon = props.faIcon, classes = "fas fa-2x text-gray-300")
            }
        }
    }
}
