package org.cqfn.save.frontend.components.views

import kotlinext.js.jso
import react.PropsWithChildren
import react.RBuilder
import react.State
import react.dom.div
import react.dom.p
import react.router.dom.Link

/**
 * A [RComponent] representing fallback page with 404 error
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class FallbackView : AbstractView<PropsWithChildren, State>(false) {
    @Suppress("ForbiddenComment")
    override fun RBuilder.render() {
        div("text-center") {
            div("error mx-auto") {
                attrs["data-text"] = "404"
                +"404"
            }
            p("lead text-gray-800 mb-5") {
                +"Page not found"
            }
            child(type = Link, props = jso()) {
                attrs.to = "/"
                +"← Back to the main page"
            }
        }
    }
}
