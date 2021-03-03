/**
 * Components for cards
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package org.cqfn.save.frontend.components.basic

import react.RProps
import react.dom.RDOMBuilder
import react.dom.div
import react.dom.i
import react.functionalComponent

import kotlinx.html.DIV

/**
 * [RProps] for card component
 */
external interface CardProps : RProps {
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
 * A functional [RComponent] for a card.
 *
 * @param contentBuilder a builder function for card content
 * @return a functional component representing a card
 */
@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
fun cardComponent(contentBuilder: RDOMBuilder<DIV>.() -> Unit) = functionalComponent<CardProps> { props ->
    props.leftBorderColor ?: run {
        props.leftBorderColor = "primary"
    }
    div("col-xl-3 col-md-6 mb-4") {
        div("card border-left-${props.leftBorderColor} shadow h-100 py-2") {
            div("card-body") {
                div("row no-gutters align-items-center") {
                    div("col mr-2") {
                        div("text-xs font-weight-bold text-primary text-uppercase mb-1") {
                            +props.header
                        }
                        div("mb-0 font-weight-bold text-gray-800") {
                            contentBuilder.invoke(this)
                        }
                    }
                    div("col-auto") {
                        i("fas ${props.faIcon} fa-2x text-gray-300") {}
                    }
                }
            }
        }
    }
}
