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
import org.cqfn.save.frontend.utils.withRouter
import org.cqfn.save.info.UserInfo

import org.w3c.dom.HTMLElement
import org.w3c.dom.url.URLSearchParams
import org.w3c.fetch.Headers
import react.*
import react.dom.div
import react.dom.render
import react.router.Route
import react.router.Routes
import react.router.dom.HashRouter

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
            val userInfoNew: UserInfo? = get("${window.location.origin}/sec/user", headers).run {
                if (ok) decodeFromJsonString() else null
            }
            userInfoNew?.let {
                setState {
                    userInfo = userInfoNew
                }
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
                    Routes {
                        Route {
                            attrs {
                                path = "/"
                                element = buildElement {
                                    child(WelcomeView::class) {
                                        attrs.userInfo = state.userInfo
                                    }
                                }
                            }
                        }

                        Route {
                            attrs {
                                path = "/creation"
                                element = buildElement {
                                    child(CreationView::class) {
                                        attrs.userInfo = state.userInfo
                                    }
                                }
                            }
                        }

                        Route {
                            attrs {
                                path = "/projects"
                                element = buildElement {
                                    child(CollectionView::class) {}
                                }
                            }
                        }

                        Route {
                            attrs {
                                path = "/:owner/:name"
                                element = buildElement {
                                    child(withRouter { _, params ->
                                        child(ProjectView::class) {
                                            attrs.name = params["name"]!!
                                            attrs.owner = params["owner"]!!
                                        }
                                    })
                                }
                            }
                        }

                        Route {
                            attrs {
                                path = "/:owner/:name/history"
                                element = buildElement {
                                    child(withRouter { _, params ->
                                        child(HistoryView::class) {
                                            attrs.name = params["name"]!!
                                            attrs.owner = params["owner"]!!
                                        }
                                    })
                                }
                            }
                        }

                        Route {
                            attrs {
                                path = "/:owner/:name/history/execution/:executionId"
                                element = buildElement {
                                    child(withRouter { location, params ->
                                        child(ExecutionView::class) {
                                            attrs.executionId = params["executionId"]!!
                                            attrs.status = URLSearchParams(location.search).get("status")?.let(
                                                TestResultStatus::valueOf
                                            )
                                        }
                                    })
                                }
                            }
                        }

                        Route {
                            attrs {
                                path = "/:owner/:name/history/execution/:executionId/details/:testSuiteName/:pluginName/:testFilePath/*"
                                element = buildElement {
                                    child(testExecutionDetailsView()) {}
                                }
                            }
                        }

                        Route {
                            attrs {
                                path = "*"
                                element = buildElement {
                                    child(FallbackView::class) {}
                                }
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

    render(document.getElementById("wrapper") as HTMLElement) {
        child(App::class) {}
    }
}
