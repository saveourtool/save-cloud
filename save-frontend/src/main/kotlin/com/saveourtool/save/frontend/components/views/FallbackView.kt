/**
 * Support rendering something as a fallback
 */

package com.saveourtool.save.frontend.components.views

import csstype.ClassName
import react.ChildrenBuilder
import react.Props
import react.State
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.p
import react.router.dom.Link

import kotlinx.browser.window

/**
 * Props of fallback component
 */
external interface FallbackViewProps : Props {
    /**
     * Text displayed in big letters
     */
    var bigText: String?

    /**
     * Small text for more vebose description
     */
    var smallText: String?

    /**
     * Whether link to the start page should be a `<a href=>` (if false) or react-routers `Link` (if true).
     * If this component is placed outside react-router's Router, then `Link` will be inaccessible.
     */
    var withRouterLink: Boolean?
}

/**
 * A Component representing fallback page with 404 error
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class FallbackView : AbstractView<FallbackViewProps, State>(false) {
    @Suppress("ForbiddenComment")
    override fun ChildrenBuilder.render() {
        div {
            className = ClassName("text-center")
            div {
                className = ClassName("error mx-auto")
                props.bigText?.let {
                    asDynamic()["data-text"] = it
                }
                +"${props.bigText}"
            }
            p {
                className = ClassName("lead text-gray-800 mb-5")
                +"${props.smallText}"
            }
            if (props.withRouterLink == true) {
                Link {
                    to = "/"
                    +"← Back to the main page"
                }
            } else {
                a {
                    href = "${window.location.origin}/"
                    +"← Back to the main page"
                }
            }
        }
    }
}
