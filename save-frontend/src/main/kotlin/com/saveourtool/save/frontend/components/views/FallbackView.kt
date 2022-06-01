/**
 * Support rendering something as a fallback
 */

package com.saveourtool.save.frontend.components.views

import react.Props
import react.RBuilder
import react.State
import react.dom.a
import react.dom.div
import react.dom.p
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
 * A [RComponent] representing fallback page with 404 error
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class FallbackView : AbstractView<FallbackViewProps, State>(false) {
    @Suppress("ForbiddenComment")
    override fun RBuilder.render() {
        div("text-center") {
            div("error mx-auto") {
                props.bigText?.let { attrs["data-text"] = it }
                +"${props.bigText}"
            }
            p("lead text-gray-800 mb-5") {
                +"${props.smallText}"
            }
            if (props.withRouterLink == true) {
                Link {
                    attrs.to = "/"
                    +"← Back to the main page"
                }
            } else {
                a(href = "${window.location.origin}/") {
                    +"← Back to the main page"
                }
            }
        }
    }
}
