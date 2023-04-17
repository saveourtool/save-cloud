/**
 * Carousel extension function
 */

package com.saveourtool.save.frontend.components.basic

import csstype.ClassName
import csstype.invert
import js.core.jso
import react.CSSProperties
import react.ChildrenBuilder
import react.dom.aria.AriaRole
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.span

private const val INVERT_TO_OPPOSITE = 100

/**
 * @param items
 * @param carouselBodyId
 * @param styles
 * @param displayItem
 */
fun <T : Any> ChildrenBuilder.carousel(
    items: List<T>,
    carouselBodyId: String,
    styles: CSSProperties? = null,
    displayItem: ChildrenBuilder.(T) -> Unit,
) {
    div {
        className = ClassName("carousel slide card flex-md-row box-shadow")
        style = styles
        id = carouselBodyId
        asDynamic()["data-ride"] = "carousel"

        div {
            className = ClassName("carousel-inner my-auto")
            items.forEachIndexed { i, item ->
                val classes = if (i == 0) "active" else ""
                slide(classes) { displayItem(item) }
            }
        }
        carouselArrows(carouselBodyId)
    }
}

/**
 * @param classes
 * @param displayItem
 */
fun ChildrenBuilder.slide(classes: String, displayItem: ChildrenBuilder.() -> Unit) {
    div {
        className = ClassName("carousel-item $classes")
        div {
            className = ClassName("row mt-auto")
            displayItem()
        }
    }
}

/**
 * @param carouselBodyId
 */
fun ChildrenBuilder.carouselArrows(carouselBodyId: String) {
    a {
        style = jso { filter = invert(INVERT_TO_OPPOSITE) }
        className = ClassName("carousel-control-prev ")
        href = "#$carouselBodyId"
        role = "button".unsafeCast<AriaRole>()
        asDynamic()["data-slide"] = "prev"
        span { className = ClassName("carousel-control-prev-icon") }
    }
    a {
        style = jso { filter = invert(INVERT_TO_OPPOSITE) }
        className = ClassName("carousel-control-next")
        href = "#$carouselBodyId"
        role = "button".unsafeCast<AriaRole>()
        asDynamic()["data-slide"] = "next"
        span { className = ClassName("carousel-control-next-icon") }
    }
}
