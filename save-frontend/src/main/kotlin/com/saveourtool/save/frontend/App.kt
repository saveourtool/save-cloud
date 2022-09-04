/**
 * Main entrypoint for SAVE frontend
 */

package com.saveourtool.save.frontend

import com.saveourtool.save.*
import com.saveourtool.save.domain.Role
import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.entities.benchmarks.BenchmarkCategoryEnum
import com.saveourtool.save.execution.TestExecutionFilters
import com.saveourtool.save.frontend.components.*
import com.saveourtool.save.frontend.components.basic.scrollToTopButton
import com.saveourtool.save.frontend.components.views.*
import com.saveourtool.save.frontend.components.views.contests.ContestListView
import com.saveourtool.save.frontend.components.views.projectcollection.CollectionView
import com.saveourtool.save.frontend.components.views.usersettings.UserSettingsEmailMenuView
import com.saveourtool.save.frontend.components.views.usersettings.UserSettingsOrganizationsMenuView
import com.saveourtool.save.frontend.components.views.usersettings.UserSettingsProfileMenuView
import com.saveourtool.save.frontend.components.views.usersettings.UserSettingsTokenMenuView
import com.saveourtool.save.frontend.components.views.welcome.WelcomeView
import com.saveourtool.save.frontend.externals.modal.ReactModal
import com.saveourtool.save.frontend.http.getUser
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes

import csstype.ClassName
import org.w3c.dom.HTMLElement
import org.w3c.dom.url.URLSearchParams
import react.*
import react.dom.client.createRoot
import react.dom.html.ReactHTML.div
import react.router.Navigate
import react.router.Route
import react.router.Routes
import react.router.dom.HashRouter

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.js.get
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
            filters = URLSearchParams(location.search).let { params ->
                TestExecutionFilters(
                    status = params.get("status")?.let { TestResultStatus.valueOf(it) },
                    fileName = params.get("fileName"),
                    testSuite = params.get("testSuite"),
                    tag = params.get("tag")
                )
            }
        }
    }
    private val contestView: FC<Props> = withRouter { _, params ->
        ContestView::class.react {
            currentUserInfo = state.userInfo
            currentContestName = params["contestName"]
        }
    }
    private val contestExecutionView: FC<Props> = withRouter { _, params ->
        ContestExecutionView::class.react {
            currentUserInfo = state.userInfo
            contestName = params["contestName"]!!
            organizationName = params["organizationName"]!!
            projectName = params["projectName"]!!
        }
    }
    private val organizationView: FC<Props> = withRouter { _, params ->
        OrganizationView::class.react {
            organizationName = params["owner"]!!
            currentUserInfo = state.userInfo
        }
    }
    private val fallbackNode = FallbackView::class.react.create {
        bigText = "404"
        smallText = "Page not found"
        withRouterLink = false
    }

    init {
        state.userInfo = null
    }

    override fun componentDidMount() {
        getUser()
    }

    @Suppress("TOO_LONG_FUNCTION", "NULLABLE_PROPERTY_TYPE")
    private fun getUser() {
        scope.launch {
            val userName: String? = get(
                "${window.location.origin}/sec/user",
                jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler
            ).run {
                val responseText = text().await()
                if (!ok || responseText == "null") null else responseText
            }

            val globalRole: Role? = get(
                "${window.location.origin}/api/$v1/users/global-role",
                jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler
            ).run {
                val responseText = text().await()
                if (!ok || responseText == "null") null else Json.decodeFromString(responseText)
            }

            val user: UserInfo? = userName
                ?.let { getUser(it) }

            val userInfoNew: UserInfo? = user?.copy(globalRole = globalRole) ?: userName?.let { UserInfo(name = userName, globalRole = globalRole) }

            userInfoNew?.let {
                setState {
                    userInfo = userInfoNew
                }
            }
        }
    }

    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR", "TOO_LONG_FUNCTION", "LongMethod")
    override fun ChildrenBuilder.render() {
        HashRouter {
            requestModalHandler {
                userInfo = state.userInfo

                withRouter<Props> { location, _ ->
                    if (state.userInfo?.isActive == false && !location.pathname.startsWith("/${FrontendRoutes.REGISTRATION.path}")) {
                        Navigate {
                            to = "/${FrontendRoutes.REGISTRATION.path}"
                            replace = false
                        }
                    } else if (state.userInfo?.isActive == true && location.pathname.startsWith("/${FrontendRoutes.REGISTRATION.path}")) {
                        Navigate {
                            to = "/${FrontendRoutes.PROJECTS.path}"
                            replace = false
                        }
                    }
                }()

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
                                    path = "/${FrontendRoutes.AWESOME_BENCHMARKS.path}"
                                    element = AwesomeBenchmarksView::class.react.create()
                                }

                                createRoutersWithPathAndEachListItem<BenchmarkCategoryEnum>("archive/${FrontendRoutes.AWESOME_BENCHMARKS.path}",
                                    routeElement = AwesomeBenchmarksView::class.react.create())

                                Route {
                                    path = "/${FrontendRoutes.REGISTRATION.path}"
                                    element = RegistrationView::class.react.create() {
                                        userInfo = state.userInfo
                                    }
                                }

                                Route {
                                    path = "/${FrontendRoutes.CONTESTS.path}/:contestName"
                                    element = contestView.create()
                                }

                                createRoutersWithPathAndEachListItem<ContestMenuBar>("contests/${FrontendRoutes.CONTESTS.path}/:contestName", contestView.create())

                                Route {
                                    path = "/${FrontendRoutes.CONTESTS.path}/:contestName/:organizationName/:projectName"
                                    element = contestExecutionView.create()
                                }

                                Route {
                                    path = "/${state.userInfo?.name}/${FrontendRoutes.SETTINGS_PROFILE.path}"
                                    element = state.userInfo?.name?.let {
                                        UserSettingsProfileMenuView::class.react.create {
                                            userName = it
                                        }
                                    } ?: fallbackNode
                                }

                                Route {
                                    path = "/${state.userInfo?.name}/${FrontendRoutes.SETTINGS_EMAIL.path}"
                                    element = state.userInfo?.name?.let {
                                        UserSettingsEmailMenuView::class.react.create {
                                            userName = it
                                        }
                                    } ?: fallbackNode
                                }

                                Route {
                                    path = "/${state.userInfo?.name}/${FrontendRoutes.SETTINGS_TOKEN.path}"
                                    element = state.userInfo?.name?.let {
                                        UserSettingsTokenMenuView::class.react.create {
                                            userName = it
                                        }
                                    } ?: fallbackNode
                                }

                                Route {
                                    path = "/${state.userInfo?.name}/${FrontendRoutes.SETTINGS_ORGANIZATIONS.path}"
                                    element = state.userInfo?.name?.let {
                                        UserSettingsOrganizationsMenuView::class.react.create {
                                            userName = it
                                        }
                                    } ?: fallbackNode
                                }

                                Route {
                                    path = "/${FrontendRoutes.CREATE_PROJECT.path}"
                                    element = CreationView::class.react.create()
                                }

                                Route {
                                    path = "/${FrontendRoutes.CREATE_ORGANIZATION.path}"
                                    element = CreateOrganizationView::class.react.create()
                                }

                                Route {
                                    path = "/${FrontendRoutes.PROJECTS.path}"
                                    element = CollectionView::class.react.create {
                                        currentUserInfo = state.userInfo
                                    }
                                }

                                Route {
                                    path = "/${FrontendRoutes.CONTESTS.path}"
                                    element = ContestListView::class.react.create {
                                        currentUserInfo = state.userInfo
                                    }
                                }

                                Route {
                                    path = "/:owner"
                                    element = organizationView.create()
                                }

                                createRoutersWithPathAndEachListItem<OrganizationMenuBar>("/organization/:owner", organizationView.create())

                                Route {
                                    path = "/:owner/:name/history"
                                    element = historyView.create()
                                }

                                Route {
                                    path = "/:owner/:name"
                                    element = projectView.create()
                                }

                                createRoutersWithPathAndEachListItem<ProjectMenuBar>("/project/:owner/:name", projectView.create())

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

/**
 * The function creates routers with the given [href] and ending of string with all the elements given Enum<T>
 *
 * @param href
 * @param routeElement
 */
inline fun <reified T : Enum<T>>ChildrenBuilder.createRoutersWithPathAndEachListItem(href: String, routeElement: ReactNode?) {
    enumValues<T>().map { it.name.lowercase() }.forEach { item ->
        Route {
            path = "$href/$item"
            element = routeElement
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
