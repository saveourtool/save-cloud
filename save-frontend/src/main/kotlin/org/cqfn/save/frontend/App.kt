/**
 * Main entrypoint for SAVE frontend
 */

package org.cqfn.save.frontend

import org.cqfn.save.frontend.components.Footer
import org.cqfn.save.frontend.components.TopBar
import org.cqfn.save.frontend.components.basic.scrollToTopButton
import org.cqfn.save.frontend.components.views.CollectionView
import org.cqfn.save.frontend.components.views.ExecutionProps
import org.cqfn.save.frontend.components.views.ExecutionView
import org.cqfn.save.frontend.components.views.FallbackView
import org.cqfn.save.frontend.components.views.HistoryView
import org.cqfn.save.frontend.components.views.ProjectRouteProps
import org.cqfn.save.frontend.components.views.ProjectView
import org.cqfn.save.frontend.components.views.toProject
import org.cqfn.save.frontend.externals.modal.ReactModal

import org.w3c.dom.HTMLElement
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.child
import react.dom.div
import react.dom.render
import react.router.dom.hashRouter
import react.router.dom.route
import react.router.dom.switch

import kotlinx.browser.document
import kotlinx.html.id

/**
 * Top-level state of the whole App
 */
external interface AppState : RState {
    /**
     * Currently logged in user or null
     */
    var userName: String?
}

/**
 * MAin component for the whole App
 */
class App : RComponent<RProps, AppState>() {
    init {
        state.userName = "User Name"
    }

    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR", "TOO_LONG_FUNCTION")
    override fun RBuilder.render() {
        hashRouter {
            div("d-flex flex-column") {
                attrs.id = "content-wrapper"
                route<RProps>("*") { routeResultProps ->
                    // needs to be wrapped in `route` to have access to pathname; we place it outside of `switch` to render always and unconditionally
                    child(TopBar::class) {
                        attrs {
                            pathname = routeResultProps.location.pathname
                            userName = state.userName
                        }
                    }
                }
                div("container-fluid") {
                    switch {
                        route("/", exact = true, component = CollectionView::class)
                        route<ProjectRouteProps>("/:type/:owner/:name", exact = true) { routeResultProps ->
                            child(ProjectView::class) {
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
                        route("*", component = FallbackView::class)
                    }
                }
                child(Footer::class) {}
            }
        }
        child(scrollToTopButton()) {}
    }
}

@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
fun main() {
    kotlinext.js.require("../scss/save-frontend.scss")  // this is needed for webpack to include resource
    kotlinext.js.require("bootstrap")  // this is needed for webpack to include bootstrap
    ReactModal.setAppElement(document.getElementById("wrapper") as HTMLElement)  // required for accessibility in react-modal

    render(document.getElementById("wrapper")) {
        child(App::class) {}
    }
}
