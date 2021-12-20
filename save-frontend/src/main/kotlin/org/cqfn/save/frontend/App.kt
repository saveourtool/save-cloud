/**
 * Main entrypoint for SAVE frontend
 */

package org.cqfn.save.frontend

import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.frontend.components.Footer
import org.cqfn.save.frontend.components.basic.scrollToTopButton
import org.cqfn.save.frontend.components.topBar
import org.cqfn.save.frontend.components.views.CollectionView
import org.cqfn.save.frontend.components.views.CreationView
import org.cqfn.save.frontend.components.views.ExecutionView
import org.cqfn.save.frontend.components.views.FallbackView
import org.cqfn.save.frontend.components.views.HistoryView
import org.cqfn.save.frontend.components.views.ProjectView
import org.cqfn.save.frontend.components.views.WelcomeView
import org.cqfn.save.frontend.components.views.testExecutionDetailsView
import org.cqfn.save.frontend.externals.fontawesome.faAngleUp
import org.cqfn.save.frontend.externals.fontawesome.faCheck
import org.cqfn.save.frontend.externals.fontawesome.faCogs
import org.cqfn.save.frontend.externals.fontawesome.faExclamationTriangle
import org.cqfn.save.frontend.externals.fontawesome.faFile
import org.cqfn.save.frontend.externals.fontawesome.faQuestionCircle
import org.cqfn.save.frontend.externals.fontawesome.faSignOutAlt
import org.cqfn.save.frontend.externals.fontawesome.faTimesCircle
import org.cqfn.save.frontend.externals.fontawesome.faUpload
import org.cqfn.save.frontend.externals.fontawesome.faUser
import org.cqfn.save.frontend.externals.fontawesome.fas
import org.cqfn.save.frontend.externals.fontawesome.library
import org.cqfn.save.frontend.externals.modal.ReactModal
import org.cqfn.save.frontend.utils.decodeFromJsonString
import org.cqfn.save.frontend.utils.get
import org.cqfn.save.info.UserInfo

import org.w3c.dom.HTMLElement
import org.w3c.dom.url.URLSearchParams
import org.w3c.fetch.Headers
import react.*
import react.dom.div
import react.dom.render
import react.router.dom.HashRouter
import react.router.dom.Route
import react.router.dom.Switch

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.html.id

/**
 * Top-level state of the whole App
 */
external interface AppState : State {
    /**
     * Currently logged in user or null
     */
    var userInfo: UserInfo?
}

/**
 * Main component for the whole App
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class App : RComponent<PropsWithChildren, AppState>() {
    init {
        state.userInfo = null
    }

    private fun getUser() {
        GlobalScope.launch {
            val headers = Headers().also { it.set("Accept", "application/json") }
            val userInfoNew: UserInfo? = get("${window.location.origin}/sec/user", headers).decodeFromJsonString()
            setState {
                userInfo = userInfoNew
            }
        }
    }

    override fun componentDidMount() {
        getUser()
    }

    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR", "TOO_LONG_FUNCTION", "LongMethod")
    override fun RBuilder.render() {
        HashRouter {
            div("d-flex flex-column") {
                attrs.id = "content-wrapper"
                child(topBar()) {
                    attrs {
                        userInfo = state.userInfo
                    }
                }

                div("container-fluid") {
                    Switch {
                        Route {
                            attrs {
                                path = arrayOf("/")
                                exact = true
                                render = { routeResultProps ->
                                    buildElement {
                                        child(WelcomeView::class) {
                                            attrs.userInfo = state.userInfo
                                        }
                                    }
                                }
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
                                path = arrayOf("/projects")
                                exact = true
                                component = CollectionView::class.react
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
                                path = arrayOf("/:owner/:name/history/execution/:executionId")
                                exact = true
                                render = { props ->
                                    buildElement {
                                        child(ExecutionView::class) {
                                            attrs.executionId = props.match.params["executionId"]!!
                                            attrs.status = URLSearchParams(props.location.search).get("status")?.let(
                                                TestResultStatus::valueOf
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Route {
                            attrs {
                                path =
                                        arrayOf("/:owner/:name/history/execution/:executionId/details/:testSuiteName/:pluginName/:testFilePath+")
                                exact = false  // all paths parts under testFilePath should be captured
                            }
                            child(testExecutionDetailsView()) {}
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
    library.add(
        fas, faUser, faCogs, faSignOutAlt, faAngleUp, faCheck, faExclamationTriangle, faTimesCircle, faQuestionCircle,
        faUpload, faFile
    )
    ReactModal.setAppElement(document.getElementById("wrapper") as HTMLElement)  // required for accessibility in react-modal

    render(document.getElementById("wrapper")) {
        child(App::class) {}
    }
}
