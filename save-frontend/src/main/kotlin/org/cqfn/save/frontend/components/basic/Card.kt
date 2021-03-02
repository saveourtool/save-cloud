/**
 * Components for cards
 */

package org.cqfn.save.frontend.components.basic

import kotlinx.html.DIV
import react.RComponent
import react.RProps
import react.dom.RDOMBuilder
import react.dom.div
import react.dom.i
import react.functionalComponent

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
 */
@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
fun cardComponent(contentBuilder: RDOMBuilder<DIV>.() -> Unit) = functionalComponent<CardProps> { props ->
    if (props.leftBorderColor == null) props.leftBorderColor = "primary"
    div("col-xl-3 col-md-6 mb-4") {
        div("card border-left-${props.leftBorderColor} shadow h-100 py-2") {
            div("card-body") {
                div("row no-gutters align-items-center") {
                    div("col mr-2") {
                        div("text-xs font-weight-bold text-primary text-uppercase mb-1") {
                            +props.header
                        }
                        div("h5 mb-0 font-weight-bold text-gray-800") {
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
