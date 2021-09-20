/**
 * Components for cards
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package org.cqfn.save.frontend.components.basic

import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon

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
     * Color of card's left border, look in bootstrap for available options.
     * Default value: `"primary"`.
     */
    var leftBorderColor: String?

    /**
     * Header of the card
     */
    var header: String

    /**
     * font-awesome class to be used as an icon
     */
    var faIcon: String
}

/**
 * A functional `RComponent` for a card.
 *
 * @param contentBuilder a builder function for card content
 * @return a functional component representing a card
 */
@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
fun cardComponent(contentBuilder: RDOMBuilder<DIV>.() -> Unit) = fc<CardProps> { props ->
    div("card-body") {
        div("row no-gutters align-items-center") {
            div("col mr-2") {
                div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                    +props.header
                }
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
}
