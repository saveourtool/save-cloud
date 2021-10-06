/**
 * Main entrypoint for SAVE frontend
 */

package org.cqfn.save.frontend

import org.cqfn.save.frontend.components.Footer
import org.cqfn.save.frontend.components.TopBar
import org.cqfn.save.frontend.components.basic.scrollToTopButton
import org.cqfn.save.frontend.components.views.CollectionView
import org.cqfn.save.frontend.components.views.CreationView
import org.cqfn.save.frontend.components.views.ExecutionView
import org.cqfn.save.frontend.components.views.FallbackView
import org.cqfn.save.frontend.components.views.HistoryView
import org.cqfn.save.frontend.components.views.ProjectView
import org.cqfn.save.frontend.externals.fontawesome.faAngleUp
import org.cqfn.save.frontend.externals.fontawesome.faCheck
import org.cqfn.save.frontend.externals.fontawesome.faCogs
import org.cqfn.save.frontend.externals.fontawesome.faExclamationTriangle
import org.cqfn.save.frontend.externals.fontawesome.faQuestionCircle
import org.cqfn.save.frontend.externals.fontawesome.faSignOutAlt
import org.cqfn.save.frontend.externals.fontawesome.faTimesCircle
import org.cqfn.save.frontend.externals.fontawesome.faUpload
import org.cqfn.save.frontend.externals.fontawesome.faUser
import org.cqfn.save.frontend.externals.fontawesome.fas
import org.cqfn.save.frontend.externals.fontawesome.library
import org.cqfn.save.frontend.externals.modal.ReactModal

import org.w3c.dom.HTMLElement
import react.PropsWithChildren
import react.RBuilder
import react.RComponent
import react.State
import react.buildElement
import react.child
import react.dom.div
import react.dom.render
import react.react
import react.router.dom.HashRouter
import react.router.dom.Route
import react.router.dom.Switch
import react.router.dom.withRouter

import kotlinx.browser.document
import kotlinx.html.id
import org.cqfn.save.frontend.externals.fontawesome.faFile

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

    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR", "TOO_LONG_FUNCTION", "LongMethod")
    override fun RBuilder.render() {
        HashRouter {
            div("d-flex flex-column") {
                attrs.id = "content-wrapper"
                child(withRouter(TopBar::class)) {
                    // todo: use `useRouterMatch` inside `TopBar` instead of invoking `withRouter` here?
                    attrs {
                        userName = state.userName
                    }
                }
                div("container-fluid") {
                    Switch {
                        Route {
                            attrs {
                                path = arrayOf("/")
                                exact = true
                                component = CollectionView::class.react
                            }
                        }
                        Route {
                            attrs {
                                path = arrayOf("/creation")
                                exact = true
                                component = CreationView::class.react
                            }
                        }
                        Route {
                            attrs {
                                path = arrayOf("/:owner/:name")
                                exact = true
                                render = { routeResultProps ->
                                    buildElement {
                                        child(ProjectView::class) {
                                            attrs.name = routeResultProps.match.params["name"]!!
                                            attrs.owner = routeResultProps.match.params["owner"]!!
                                        }
                                    }
                                }
                            }
                        }
                        Route {
                            attrs {
                                path = arrayOf("/:owner/:name/history")
                                exact = true
                                render = { routeResultProps ->
                                    buildElement {
                                        child(HistoryView::class) {
                                            attrs.name = routeResultProps.match.params["name"]!!
                                            attrs.owner = routeResultProps.match.params["owner"]!!
                                        }
                                    }
                                }
                            }
                        }
                        Route {
                            attrs {
                                path = arrayOf("/:owner/:name/history/:executionId")
                                render = { props ->
                                    buildElement {
                                        // executionId might be `latest`
                                        child(ExecutionView::class) {
                                            attrs.executionId = props.match.params["executionId"]!!
                                        }
                                    }
                                }
                            }
                        }
                        Route {
                            attrs {
                                path = arrayOf("*")
                                component = FallbackView::class.react
                            }
                        }
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
    library.add(fas, faUser, faCogs, faSignOutAlt, faAngleUp, faCheck, faExclamationTriangle, faTimesCircle, faQuestionCircle,
        faUpload, faFile
    )
    ReactModal.setAppElement(document.getElementById("wrapper") as HTMLElement)  // required for accessibility in react-modal

    render(document.getElementById("wrapper")) {
        child(App::class) {}
    }
}
