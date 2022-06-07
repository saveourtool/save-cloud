/**
 * Main entrypoint for SAVE frontend
 */

package com.saveourtool.save.frontend

import com.saveourtool.save.domain.Role
import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.frontend.components.*
import com.saveourtool.save.frontend.components.basic.scrollToTopButton
import com.saveourtool.save.frontend.components.views.*
import com.saveourtool.save.frontend.components.views.usersettingsview.UserSettingsEmailMenuView
import com.saveourtool.save.frontend.components.views.usersettingsview.UserSettingsOrganizationsMenuView
import com.saveourtool.save.frontend.components.views.usersettingsview.UserSettingsProfileMenuView
import com.saveourtool.save.frontend.components.views.usersettingsview.UserSettingsTokenMenuView
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.externals.modal.ReactModal
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.v1

import org.w3c.dom.HTMLElement
import org.w3c.dom.url.URLSearchParams
import org.w3c.fetch.Headers
import react.FC
import react.Props
import react.PropsWithChildren
import react.RBuilder
import react.State
import react.buildElement
import react.dom.div
import react.dom.render
import react.react
import react.router.Route
import react.router.Routes
import react.router.dom.HashRouter
import react.setState

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.html.id
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val scrollToTopButton = scrollToTopButton()

internal val topBarComponent = topBar()

private val testExecutionDetailsView = testExecutionDetailsView()

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
class App : ComponentWithScope<PropsWithChildren, AppState>() {
    private val projectView: FC<Props> = withRouter { _, params ->
        child(ProjectView::class) {
            attrs.name = params["name"]!!
            attrs.owner = params["owner"]!!
            attrs.currentUserInfo = state.userInfo
        }
    }
    private val historyView: FC<Props> = withRouter { _, params ->
        child(HistoryView::class) {
            attrs.name = params["name"]!!
            attrs.organizationName = params["owner"]!!
        }
    }
    private val executionView: FC<Props> = withRouter { location, params ->
        child(ExecutionView::class) {
            attrs.executionId = params["executionId"]!!
            attrs.status = URLSearchParams(location.search).get("status")?.let(
                TestResultStatus::valueOf
            )
        }
    }
    init {
        state.userInfo = null
    }

    private fun getUser() {
        scope.launch {
            val userInfoNew: UserInfo? = get(
                "${window.location.origin}/sec/user",
                Headers().also { it.set("Accept", "application/json") },
                loadingHandler = ::classLoadingHandler,
                responseHandler = ::noopResponseHandler
            ).run {
                val responseText = text().await()
                if (!ok || responseText == "null") null else Json.decodeFromString(responseText)
            }

            val globalRole: Role? = get(
                "${window.location.origin}/api/$v1/users/global-role",
                Headers().also { it.set("Accept", "application/json") },
                loadingHandler = ::classLoadingHandler,
                responseHandler = ::noopResponseHandler
            ).run {
                val responseText = text().await()
                if (!ok || responseText == "null") null else Json.decodeFromString(responseText)
            }
            userInfoNew?.let {
                setState {
                    userInfo = userInfoNew.copy(globalRole = globalRole)
                }
            }
        }
    }

    override fun componentDidMount() {
        getUser()
    }

    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR", "TOO_LONG_FUNCTION", "LongMethod")
    override fun RBuilder.render() {
//        while (true) { }
        HashRouter {
            requestModalHandler {
                div("d-flex flex-column") {
                    attrs.id = "content-wrapper"
                    ErrorBoundary::class.react {
                        topBarComponent {
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
                                        path = "/awesome-benchmarks"
                                        element = buildElement {
                                            child(AwesomeBenchmarksView::class) {}
                                        }
                                    }
                                }

                                Route {
                                    attrs {
                                        path = "/:user/settings/profile"
                                        element = buildElement {
                                            child(UserSettingsProfileMenuView::class) {
                                                attrs.userName = state.userInfo?.name
                                            }
                                        }
                                    }
                                }

                                Route {
                                    attrs {
                                        path = "/:user/settings/email"
                                        element = buildElement {
                                            child(UserSettingsEmailMenuView::class) {
                                                attrs.userName = state.userInfo?.name
                                            }
                                        }
                                    }
                                }

                                Route {
                                    attrs {
                                        path = "/:user/settings/token"
                                        element = buildElement {
                                            child(UserSettingsTokenMenuView::class) {
                                                attrs.userName = state.userInfo?.name
                                            }
                                        }
                                    }
                                }

                                Route {
                                    attrs {
                                        path = "/:user/settings/organizations"
                                        element = buildElement {
                                            child(UserSettingsOrganizationsMenuView::class) {
                                                attrs.userName = state.userInfo?.name
                                            }
                                        }
                                    }
                                }

                                Route {
                                    attrs {
                                        path = "/creation"
                                        element = buildElement {
                                            child(CreationView::class) {}
                                        }
                                    }
                                }

                                Route {
                                    attrs {
                                        path = "/createOrganization"
                                        element = buildElement {
                                            child(CreateOrganizationView::class) {}
                                        }
                                    }
                                }

                                Route {
                                    attrs {
                                        path = "/projects"
                                        element = buildElement {
                                            child(CollectionView::class) {
                                                attrs.currentUserInfo = state.userInfo
                                            }
                                        }
                                    }
                                }

                                Route {
                                    attrs {
                                        path = "/:owner"
                                        element = buildElement {
                                            child(withRouter { _, params ->
                                                child(OrganizationView::class) {
                                                    attrs.organizationName = params["owner"]!!
                                                    attrs.currentUserInfo = state.userInfo
                                                }
                                            })
                                        }
                                    }
                                }

                                Route {
                                    attrs {
                                        path = "/:owner/:name"
                                        element = buildElement {
                                            child(projectView)
                                        }
                                    }
                                }

                                Route {
                                    attrs {
                                        path = "/:owner/:name/history"
                                        element = buildElement {
                                            child(historyView)
                                        }
                                    }
                                }

                                Route {
                                    attrs {
                                        path = "/:owner/:name/history/execution/:executionId"
                                        element = buildElement {
                                            child(executionView)
                                        }
                                    }
                                }

                                Route {
                                    attrs {
                                        // Since testFilePath can represent the nested path, we catch it as *
                                        path =
                                            "/:owner/:name/history/execution/:executionId/details/:testSuiteName/:pluginName/*"
                                        element = buildElement {
                                            testExecutionDetailsView()
                                        }
                                    }
                                }

                                Route {
                                    attrs {
                                        path = "*"
                                        element = buildElement {
                                            FallbackView::class.react {
                                                attrs {
                                                    bigText = "404"
                                                    smallText = "Page not found"
                                                    withRouterLink = true
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        child(Footer::class) {}
                    }
                }
            }
            child(scrollToTopButton) {}
        }
    }
}

@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
fun main() {
    /** Workaround for issue: https://youtrack.jetbrains.com/issue/KT-31888 */
    if (window.asDynamic().__karma__) {
        return
    }

    kotlinext.js.require("../scss/save-frontend.scss")  // this is needed for webpack to include resource
    kotlinext.js.require("bootstrap")  // this is needed for webpack to include bootstrap
    ReactModal.setAppElement(document.getElementById("wrapper") as HTMLElement)  // required for accessibility in react-modal

    render(document.getElementById("wrapper") as HTMLElement) {
        child(App::class) {}
    }
}
