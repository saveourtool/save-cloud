/**
 * Main entrypoint for SAVE frontend
 */

package org.cqfn.save.frontend

import org.cqfn.save.frontend.components.Footer
import org.cqfn.save.frontend.components.TopBar
import org.cqfn.save.frontend.components.views.CollectionView
import org.cqfn.save.frontend.components.views.ExecutionProps
import org.cqfn.save.frontend.components.views.ExecutionView
import org.cqfn.save.frontend.components.views.FallbackView
import org.cqfn.save.frontend.components.views.HistoryView
import org.cqfn.save.frontend.components.views.ProjectRouteProps
import org.cqfn.save.frontend.components.views.projectView
import org.cqfn.save.frontend.components.views.toProject
import org.cqfn.save.frontend.externals.modal.ReactModal

import org.w3c.dom.HTMLElement
import react.child
import react.dom.div
import react.dom.render
import react.router.dom.hashRouter
import react.router.dom.route
import react.router.dom.switch
import react.router.dom.withRouter

import kotlinx.browser.document
import kotlinx.html.id

@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR", "TOO_LONG_FUNCTION")
fun main() {
    kotlinext.js.require("../scss/save-frontend.scss")  // this is needed for webpack to include resource
    ReactModal.setAppElement(document.getElementById("wrapper") as HTMLElement)  // required for accessibility in react-modal

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
                        route("/", exact = true, component = CollectionView::class)
                        route<ProjectRouteProps>("/:type/:owner/:name", exact = true) { routeResultProps ->
                            child(projectView()) {
                                attrs.project = routeResultProps.match.params.toProject()
                            }
                        }
                        route<ProjectRouteProps>("/:type/:owner/:name/history", exact = true) { routeResultProps ->
                            child(HistoryView::class) {
                                attrs.project = routeResultProps.match.params.toProject()
                            }
                        }
                        route<ExecutionProps>("/:type/:owner/:name/history/:executionId") { props ->
                            // executionId might be `latest`
                            child(ExecutionView::class) {
                                attrs.executionId = props.match.params.executionId
                            }
                        }
                        route("*", FallbackView::class)
                    }
                }
                child(Footer::class) {}
            }
        }
    }
}
