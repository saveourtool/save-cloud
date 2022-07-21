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
import com.saveourtool.save.frontend.externals.modal.ReactModal
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.v1

import csstype.ClassName
import org.w3c.dom.HTMLElement
import org.w3c.dom.url.URLSearchParams
import org.w3c.fetch.Headers
import react.*
import react.dom.client.createRoot
import react.dom.html.ReactHTML.div
import react.dom.render
import react.router.Route
import react.router.Routes
import react.router.dom.HashRouter

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

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
        ProjectView::class.react {
            name = params["name"]!!
            owner = params["owner"]!!
            currentUserInfo = state.userInfo
        }
    }
    private val historyView: FC<Props> = withRouter { _, params ->
        HistoryView::class.react {
            name = params["name"]!!
            organizationName = params["owner"]!!
        }
    }
    private val executionView: FC<Props> = withRouter { location, params ->
        ExecutionView::class.react {
            executionId = params["executionId"]!!
            status = URLSearchParams(location.search).get("status")?.let(
                TestResultStatus::valueOf
            )
        }
    }
    private val contestView: FC<Props> = withRouter { _, params ->
        ContestView::class.react {
            currentUserInfo = state.userInfo
            currentContestName = params["contestName"]
        }
    }
    private val organizationView: FC<Props> = withRouter { _, params ->
        OrganizationView::class.react {
            organizationName = params["owner"]!!
            currentUserInfo = state.userInfo
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
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler
            ).run {
                val responseText = text().await()
                if (!ok || responseText == "null") null else Json.decodeFromString(responseText)
            }

            val globalRole: Role? = get(
                "${window.location.origin}/api/$v1/users/global-role",
                Headers().also { it.set("Accept", "application/json") },
                loadingHandler = ::noopLoadingHandler,
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
    override fun ChildrenBuilder.render() {
        HashRouter {
            requestModalHandler {
                div {
                    className = ClassName("d-flex flex-column")
                    id = "content-wrapper"
                    ErrorBoundary::class.react {
                        topBarComponent {
                            userInfo = state.userInfo
                        }

                        div {
                            className = ClassName("container-fluid")
                            id = "common-save-container"
                            Routes {
                                Route {
                                    path = "/"
                                    element = WelcomeView::class.react.create {
                                        userInfo = state.userInfo
                                    }
                                }

                                Route {
                                    path = "/awesome-benchmarks"
                                    element = AwesomeBenchmarksView::class.react.create()
                                }

                                Route {
                                    path = "/contests/:contestName"
                                    element = contestView.create()
                                }

                                Route {
                                    path = "/:user/settings/profile"
                                    element = UserSettingsProfileMenuView::class.react.create {
                                        userName = state.userInfo?.name
                                    }
                                }

                                Route {
                                    path = "/:user/settings/email"
                                    element = UserSettingsEmailMenuView::class.react.create {
                                        userName = state.userInfo?.name
                                    }
                                }

                                Route {
                                    path = "/:user/settings/token"
                                    element = UserSettingsTokenMenuView::class.react.create {
                                        userName = state.userInfo?.name
                                    }
                                }

                                Route {
                                    path = "/:user/settings/organizations"
                                    element = UserSettingsOrganizationsMenuView::class.react.create {
                                        userName = state.userInfo?.name
                                    }
                                }

                                Route {
                                    path = "/create-project"
                                    element = CreationView::class.react.create()
                                }

                                Route {
                                    path = "/create-organization"
                                    element = CreateOrganizationView::class.react.create()
                                }

                                Route {
                                    path = "/projects"
                                    element = CollectionView::class.react.create {
                                        currentUserInfo = state.userInfo
                                    }
                                }

                                Route {
                                    path = "/contests"
                                    element = ContestListView::class.react.create {
                                        currentUserInfo = state.userInfo
                                    }
                                }

                                Route {
                                    path = "/:owner"
                                    element = organizationView.create()
                                }

                                Route {
                                    path = "/:owner/:name"
                                    element = projectView.create()
                                }

                                Route {
                                    path = "/:owner/:name/history"
                                    element = historyView.create()
                                }

                                Route {
                                    path = "/:owner/:name/history/execution/:executionId"
                                    element = executionView.create()
                                }

                                Route {
                                    // Since testFilePath can represent the nested path, we catch it as *
                                    path = "/:owner/:name/history/execution/:executionId/details/:testSuiteName/:pluginName/*"
                                    element = testExecutionDetailsView.create()
                                }

                                Route {
                                    path = "*"
                                    element = FallbackView::class.react.create {
                                        bigText = "404"
                                        smallText = "Page not found"
                                        withRouterLink = true
                                    }
                                }
                            }
                        }
                        Footer::class.react()
                    }
                }
            }
            scrollToTopButton()
        }
    }
}

@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
fun main() {
    /* Workaround for issue: https://youtrack.jetbrains.com/issue/KT-31888 */
    if (window.asDynamic().__karma__) {
        return
    }

    kotlinext.js.require("../scss/save-frontend.scss")  // this is needed for webpack to include resource
    kotlinext.js.require("bootstrap")  // this is needed for webpack to include bootstrap
    ReactModal.setAppElement(document.getElementById("wrapper") as HTMLElement)  // required for accessibility in react-modal

    val mainDiv = document.getElementById("wrapper") as HTMLElement
    createRoot(mainDiv).render(App::class.react.create())
}
