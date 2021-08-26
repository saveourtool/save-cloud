/**
 * Main entrypoint for SAVE frontend
 */

package org.cqfn.save.frontend

import org.cqfn.save.frontend.components.Footer
import org.cqfn.save.frontend.components.TopBar
import org.cqfn.save.frontend.components.basic.scrollToTopButton
import org.cqfn.save.frontend.components.views.CollectionView
import org.cqfn.save.frontend.components.views.CreationView
import org.cqfn.save.frontend.components.views.ExecutionProps
import org.cqfn.save.frontend.components.views.ExecutionView
import org.cqfn.save.frontend.components.views.FallbackView
import org.cqfn.save.frontend.components.views.HistoryView
import org.cqfn.save.frontend.components.views.ProjectExecutionRouteProps
import org.cqfn.save.frontend.components.views.ProjectView
import org.cqfn.save.frontend.externals.fontawesome.faAngleUp
import org.cqfn.save.frontend.externals.fontawesome.faCheck
import org.cqfn.save.frontend.externals.fontawesome.faCogs
import org.cqfn.save.frontend.externals.fontawesome.faExclamationTriangle
import org.cqfn.save.frontend.externals.fontawesome.faSignOutAlt
import org.cqfn.save.frontend.externals.fontawesome.faUser
import org.cqfn.save.frontend.externals.fontawesome.fas
import org.cqfn.save.frontend.externals.fontawesome.library
import org.cqfn.save.frontend.externals.modal.ReactModal

import org.w3c.dom.HTMLElement
import react.PropsWithChildren
import react.RBuilder
import react.RComponent
import react.State
import react.child
import react.dom.div
import react.dom.render
import react.react
import react.router.dom.hashRouter
import react.router.dom.route
import react.router.dom.switch

import kotlinx.browser.document
import kotlinx.html.id

/**
 * Top-level state of the whole App
 */
external interface AppState : State {
    /**
     * Currently logged in user or null
     */
    var userName: String?
}

/**
 * Main component for the whole App
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class App : RComponent<PropsWithChildren, AppState>() {
    init {
        state.userName = "User Name"
    }

    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR", "TOO_LONG_FUNCTION")
    override fun RBuilder.render() {
        hashRouter {
            div("d-flex flex-column") {
                attrs.id = "content-wrapper"
                route<PropsWithChildren>("*") { routeResultProps ->
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
                        route("/", exact = true, component = CollectionView::class.react)
                        route("/creation", exact = true, component = CreationView::class.react)
                        route<ProjectExecutionRouteProps>("/:owner/:name", exact = true) { routeResultProps ->
                            child(ProjectView::class) {
                                attrs.name = routeResultProps.match.params.name
                                attrs.owner = routeResultProps.match.params.owner
                            }
                        }
                        route<ProjectExecutionRouteProps>("/:owner/:name/history", exact = true) { routeResultProps ->
                            child(HistoryView::class) {
                                attrs.name = routeResultProps.match.params.name
                                attrs.owner = routeResultProps.match.params.owner
                            }
                        }
                        route<ExecutionProps>("/:owner/:name/history/:executionId") { props ->
                            // executionId might be `latest`
                            child(ExecutionView::class) {
                                attrs.executionId = props.match.params.executionId
                            }
                        }
                        route("*", component = FallbackView::class.react)
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
    library.add(fas, faUser, faCogs, faSignOutAlt, faAngleUp, faCheck, faExclamationTriangle)
    ReactModal.setAppElement(document.getElementById("wrapper") as HTMLElement)  // required for accessibility in react-modal

    render(document.getElementById("wrapper")) {
        child(App::class) {}
    }
}
