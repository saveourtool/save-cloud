@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.contests

import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.frontend.components.basic.cardComponent
import csstype.ClassName
import react.*
import react.dom.html.ReactHTML.div

/**
 * ContestInfoMenu component props
 */
external interface ContestInfoMenuProps : Props {
    /**
     * Current contest
     */
    var contest: ContestDto?
}

/**
 * @return ReactElement
 */
@Suppress("TOO_LONG_FUNCTION")
fun contestInfoMenu(
) = fc<ContestInfoMenuProps> { props ->
    div {
        attrs.className = ClassName("d-flex justify-content-around")
        div {
            attrs.className = ClassName("col-4")
            div {
                attrs.className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Description"
            }
            div {
                attrs.className = ClassName("text-center")
                child(cardComponent(hasBg = true) { +(props.contest?.description ?: "") })
            }
        }
        div {
            attrs.className = ClassName("col-4")
            div {
                attrs.className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Public tests"
            }
            div {
                attrs.className = ClassName("text-center")
                child(cardComponent(hasBg = true, isBordered = true) {
                    +"PUBLIC TESTS WILL BE HERE"
                })
            }
        }
    }
}
