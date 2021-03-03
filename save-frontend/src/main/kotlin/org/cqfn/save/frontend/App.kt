/**
 * Main entrypoint for SAVE frontend
 */

package org.cqfn.save.frontend

import org.cqfn.save.frontend.components.FallbackView
import org.cqfn.save.frontend.components.Footer
import org.cqfn.save.frontend.components.ProjectProps
import org.cqfn.save.frontend.components.ProjectView
import org.cqfn.save.frontend.components.TopBar

import react.dom.div
import react.dom.render
import react.router.dom.hashRouter
import react.router.dom.route
import react.router.dom.switch
import react.router.dom.withRouter

import kotlinx.browser.document
import kotlinx.html.id

@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
fun main() {
    kotlinext.js.require("../scss/save-frontend.scss")  // this is needed for webpack to include resource
    render(document.getElementById("wrapper")) {
        hashRouter {
            div("d-flex flex-column") {
                attrs.id = "content-wrapper"
                route("*") {
                    // `withRouter` needs to be wrapped in `route`; we place it outside of `switch` to render always and unconditionally
                    withRouter(TopBar::class).invoke {}
                }
                div("container-fluid") {
                    switch {
                        route("/") { TODO("Collection view here") }
                        route<ProjectProps>("/:type/:owner/:name") { routeResultProps ->
                            child(ProjectView::class) {
                                attrs.name = routeResultProps.match.params.name
                                attrs.owner = routeResultProps.match.params.owner
                                attrs.description = "Todo: fetch description"
                            }
                        }
                        route("/:type/:owner/:name/history") { TODO() }
                        route("/:type/:owner/:name/history/:executionId") {
                            // executionId might be `latest`
                            TODO()
                        }
                        route("*", FallbackView::class)
                    }
                }
                child(Footer::class) {}
            }
        }
    }
}
